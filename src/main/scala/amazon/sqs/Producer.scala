package amazon.sqs

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
  def dequeue(scheduler: ScheduledExecutorService): Reader[AmazonZConfig, Process[Task, Throwable \/ List[Message]]] = Reader(config ⇒
    Process.state(1).flatMap[Task, Throwable \/ List[Message]] {
      case (get, set) ⇒
        Process.eval(
          for {
            _ ← Task.schedule((), get.millis)(scheduler)
            mess ← receiveMessageRequests(config.sqs.client, config.sqs.queue).attempt
            next = (mess | List.empty[Message]).isEmpty ? math.min(get * 2, 30000) | 1
            _ ← set(next)
          } yield mess
        ).filter(_.map(!_.isEmpty) | true)
    }
  )
  //
}
