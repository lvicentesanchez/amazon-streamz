package amazon

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import scalaz.\/

trait AsyncRequest {
  final def asyncHandler[A <: AmazonWebServiceRequest, B](f: (Throwable \/ B) ⇒ Unit): AsyncHandler[A, B] =
    new AsyncHandler[A, B] {
      override def onError(exception: Exception): Unit = {
        f(\/.left(exception))
      }

      override def onSuccess(request: A, result: B): Unit = {
        f(\/.right(result))
      }
    }
}
