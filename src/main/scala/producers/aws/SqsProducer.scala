package producers.aws

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{ Message, ReceiveMessageRequest, ReceiveMessageResult }
import config.AmazonZConfig
import java.util.concurrent.{ Callable, ScheduledExecutorService, TimeUnit }
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scalaz.\/._
import scalaz.{ Reader, \/, \/-, -\/ }
import scalaz.concurrent.Task
import scalaz.stream.Process

trait SqsProducer extends AsyncRequest {
  // Using Process.state to query SQS using an exponential backoff.
  //
  def sqsProducer(scheduler: ScheduledExecutorService): Reader[AmazonZConfig, Process[Task, Throwable \/ List[Message]]] = Reader(config ⇒
    Process.state(1).flatMap[Task, Throwable \/ List[Message]] {
      case (get, set) ⇒
        Process.eval(
          for {
            _ ← Task.schedule((), get.millis)(scheduler)
            mess ← sqsMessages(config.sqs.client, config.sqs.queue).attempt
            next = mess.flatMap {
              case Nil ⇒ -\/(new Throwable("No messages"))
              case _ ⇒ \/-(1)
            } | math.min(get * 2, 30000)
            _ ← set(next)
          } yield mess
        )
    }
  )
  //

  private[this] def sqsMessages(client: AmazonSQSAsync, queueUrl: String): Task[List[Message]] =
    for {
      request ← Task.delay[ReceiveMessageRequest](
        new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10)
      )
      receipt ← Task.async[ReceiveMessageResult] { f ⇒
        try {
          val _ = client.receiveMessageAsync(request, asyncHandler(f))
        } catch {
          case excp: Throwable ⇒
            f(-\/(excp))
        }
      }
    } yield fromReceiveMessage(receipt)

  private[this] def fromReceiveMessage(result: ReceiveMessageResult): List[Message] = {
    result.getMessages.asScala.toList
  }

  implicit class TaskSchedulingImplicit(future: Task.type) {
    def schedule[A](a: ⇒ A, delay: Duration)(implicit pool: ScheduledExecutorService): Task[A] =
      Task.async { cb ⇒
        val _ = pool.schedule(new Callable[Unit] {
          def call: Unit = cb(\/-(a))
        }, delay.toMillis, TimeUnit.MILLISECONDS)
      }
  }
}

