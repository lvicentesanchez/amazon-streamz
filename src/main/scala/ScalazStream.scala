import scala.io.StdIn
import scalaz.{ \/, -\/, \/- }
import scalaz.concurrent.Task
import scalaz.stream.{ Process, Sink }
import scalaz.stream.processes._

object ScalazStream extends App {
  // Using Process.state to query SQS using an exponential backoff.
  //
  val callbackWithBackOff: Process[Task, Throwable \/ String] = Process.state(1).flatMap[Task, Throwable \/ String] {
    case (get, set) ⇒
      Process.eval(
        for {
          text ← Task.async[String](asyncFunctin2(get, _)).attempt
          next = if (math.random > 0.10)
            math.min(get * 2, 30000)
          else
            1
          _ ← set(next)
        } yield text
      )
  }
  //
  val printlnExc: Sink[Task, Throwable] = stdOutLines.contramap(_.getMessage)
  val printlnStr: Sink[Task, String] = stdOutLines
  val getFromQueueAndLog: Process[Task, Unit] = callbackWithBackOff.drainW(printlnExc).to(printlnStr)

  // We would log any error here... but that should never happen :\
  //
  getFromQueueAndLog.run.runAsync(_ ⇒ ())
  //

  StdIn.readLine()

  def asyncFunction(f: Throwable \/ String ⇒ Unit): Unit =
    if (math.random > 0.10) {
      f(\/-("No errors"))
    } else {
      f(-\/(new Throwable("Some errors")))
    }

  def asyncFunctin2(i: Int, f: Throwable \/ String ⇒ Unit): Unit =
    f(\/-(s"No errors $i"))
}