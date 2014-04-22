package amazon.sqs

import amazon.AsyncRequest
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model._
import scala.collection.JavaConverters._
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.syntax.id._
import scalaz.syntax.std.map._

private[sqs] trait SqsOps extends AsyncRequest {
  protected[SqsOps] def receiveMessageRequests(client: AmazonSQSAsync, queueUrl: String): Task[List[Message]] =
    for {
      request ← Task.delay[ReceiveMessageRequest](
        new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10)
      )
      result ← Task.async[ReceiveMessageResult](f ⇒
        Task.Try(
          client.receiveMessageAsync(request, asyncHandler(f))
        ).swap.foreach(f compose \/.left)
      )
    } yield fromReceiveMessageResult(result)

  def deleteMessageBatchRequest(client: AmazonSQSAsync, queueUrl: String, messages: List[Message]): Task[List[String \/ Message]] = {
    val withIds = messages.map(msg ⇒ (msg.getMessageId, msg))
    for {
      request ← Task.delay[DeleteMessageBatchRequest](
        new DeleteMessageBatchRequest().withQueueUrl(queueUrl).withEntries(withIds.collect {
          case (uuid, message) ⇒ new DeleteMessageBatchRequestEntry(uuid, message.getReceiptHandle)
        }.asJava)
      )
      result ← Task.async[DeleteMessageBatchResult](f ⇒
        Task.Try(
          client.deleteMessageBatchAsync(request, asyncHandler(f))
        ).swap.foreach(f compose \/.left)
      )
    } yield fromDeleteMessageBatchResult(withIds.toMap, result)
  }

  private[this] def fromDeleteMessageBatchResult(lookUp: Map[String, Message], result: DeleteMessageBatchResult): List[String \/ Message] = {
    val failed: List[BatchResultErrorEntry] = result.getFailed.asScala.toList
    val failedMessages: List[String \/ Message] = failed.map(entry ⇒ s"Error ${entry.getCode} :: ${entry.getMessage}".left)
    val successful: List[String] = result.getSuccessful.asScala.map(_.getId).toList
    val successfulMessages: List[String \/ Message] = successful.map(lookUp.get(_)).flatten.map(_.right)

    failedMessages ++ successfulMessages
  }

  private[this] def fromReceiveMessageResult(result: ReceiveMessageResult): List[Message] = {
    result.getMessages.asScala.toList
  }
}
