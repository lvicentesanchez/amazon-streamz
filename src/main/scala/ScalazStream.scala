import scala.io.StdIn
import scalaz.{ \/, -\/, \/- }
import scalaz.concurrent.Task
import scalaz.stream.{ Process, Sink }
import scalaz.stream.processes._

object ScalazStream extends App {
  val fromCallback: Process[Task, Throwable \/ String] = Process.repeatEval(
    // We need to fork because Task.async is not stack-safe
    //
    Task.fork(
      Task.async[String] { cb ⇒
        if (math.random > 0.10) {
          cb(\/-("No errors"))
        } else {
          cb(-\/(new Throwable("Some errors")))
        }
      } attempt
    )
  )
  val printlnExc: Sink[Task, Throwable] = stdOutLines.contramap(_.getMessage)
  val printlnStr: Sink[Task, String] = stdOutLines
  val obtainFromQueueAndLog: Process[Task, Unit] = fromCallback.drainW(printlnExc).to(printlnStr)

  // We would log any error here... but that should never happen :\
  //
  obtainFromQueueAndLog.run.runAsync(_ ⇒ ())
  //

  StdIn.readLine()
}
