package amazon.sqs

import java.util.concurrent.atomic.AtomicInteger

import com.amazonaws.services.sqs.model.Message
import config.AmazonZConfig
import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.duration._
import scalaz.{ Reader, \/ }
import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.syntax.std.boolean._
import scheduler.syntax.task._

trait Producer extends SqsOps {
  // Using Process.state to query SQS using an exponential back-off.
  //
  def dequeue(scheduler: ScheduledExecutorService): Reader[AmazonZConfig, Process[Task, Throwable \/ List[Message]]] = Reader {
    config ⇒
      val delay: AtomicInteger = new AtomicInteger(1)
      Process
        .emit(()).repeat
        .flatMap[Task, Throwable \/ List[Message]] {
          _ ⇒
            Process.eval(
              for {
                prev ← Task.delay(delay.get)
                _ ← Task.schedule((), prev.millis)(scheduler)
                mess ← receiveMessageRequests(config.sqs.client, config.sqs.queue).attempt
                next = (mess | List.empty[Message]).isEmpty ? math.min(prev * 2, 30000) | 1
                _ ← Task.delay(delay.set(next))
              } yield mess
            ).filter(_.map(_.nonEmpty) | true)
        }
  }
  //
}
