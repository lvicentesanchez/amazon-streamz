package amazon.sqs

import com.amazonaws.services.sqs.model.Message
import config.AmazonZConfig
import scalaz.{ Reader, \/ }
import scalaz.concurrent.Task
import scalaz.stream.Channel
import scalaz.stream.io

trait Consumer extends SqsOps {
  val destroy: Reader[AmazonZConfig, Channel[Task, List[Message], Throwable \/ List[String \/ Message]]] = Reader(config ⇒
    io.channel(deleteMessageBatchRequest(config.sqs.client, config.sqs.queue, _).attempt)
  )
  //
}
