import scala.io.StdIn
import scalaz.-\/
import scalaz.\/-
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream.{ Process, Sink }
import scalaz.stream.async
import scalaz.stream.merge._
import scalaz.stream.async.mutable._
import scalaz.stream.processes._

object ScalazStream extends App {
  def printlnStr[A]: Sink[Task, A] = stdOutLines.contramap(_.toString)

  def queueProducers[A](nrOfJobs: Int, queue: BoundedQueue[A], producer: Process[Task, Throwable \/ A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(producer.drainW(printlnStr).to(queue.enqueue)).take(nrOfJobs))

  def dequeueWorkers[A](nrOfJobs: Int, queue: BoundedQueue[A], consumer: Sink[Task, A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(queue.dequeue.to(consumer)).take(nrOfJobs))

  val fixSizeQueue: BoundedQueue[String] = async.boundedQueue[String](1000)

  val producers = queueProducers(2, fixSizeQueue, sqsProducer(""))
  val consumers = dequeueWorkers(4, fixSizeQueue, printlnStr)

  // We would log any error here... but that should never happen :\
  //
  producers.run.runAsync(_ ⇒ ())
  consumers.run.runAsync(_ ⇒ ())
  fixSizeQueue.size.discrete.map(s ⇒ s"Size: $s").to(printlnStr).run.runAsync(_ ⇒ ())
  //

  StdIn.readLine()

  def asyncFunction(i: Int, f: Throwable \/ String ⇒ Unit): Unit =
    if (math.random > 0.50) f(\/-(s"No error $i")) else f(-\/(new Throwable(s"An error $i")))

  // Using Process.state to query SQS using an exponential backoff.
  //
  def sqsProducer: Reader[String, Process[Task, Throwable \/ String]] = Reader(string ⇒
    Process.state(1).flatMap[Task, Throwable \/ String] {
      case (get, set) ⇒
        Process.eval(
          for {
            text ← Task.async[String](asyncFunction(get, _)).attempt
            next = if (math.random > 0.10) math.min(get * 2, 30000) else 1
            _ ← set(next)
          } yield text
        )
    }
  )
}