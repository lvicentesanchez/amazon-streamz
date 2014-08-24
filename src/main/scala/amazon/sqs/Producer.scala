package amazon.sqs

import com.amazonaws.services.sqs.model.Message
import config.AmazonZConfig
import java.util.concurrent.ScheduledExecutorService
import scalaz.{ Reader, \/ }
import scalaz.concurrent.Task
import scalaz.stream.Process

trait Producer extends SqsOps {
  // Reading messages from the queue using long polling... we will wait up to 20s to get a batch of messages
  //
  def dequeue(scheduler: ScheduledExecutorService): Reader[AmazonZConfig, Process[Task, Throwable \/ List[Message]]] = Reader {
    config ⇒
      Process.repeatEval(
        receiveMessageRequests(config.sqs.client, config.sqs.queue, 20).attempt
      )
  }
  //
}
