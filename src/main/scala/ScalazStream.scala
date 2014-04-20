import config.{ AmazonZConfig, ConfigReader }
import com.typesafe.config.ConfigFactory
import java.util.concurrent.{ Executors, TimeUnit, ScheduledExecutorService, Callable }
import scala.io.StdIn
import scala.concurrent.duration._
import scalaz.{ Reader, \/, \/-, -\/ }
import scalaz.concurrent.Task
import scalaz.stream.{ Process, Sink }
import scalaz.stream.async
import scalaz.stream.merge._
import scalaz.stream.async.mutable._
import scalaz.stream.processes._
import utils._

object ScalazStream extends App with ConfigReader with SqsProducer {
  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("task-scheduler"))

  val config: AmazonZConfig = configReader(ConfigFactory.load())

  def printlnStr[A]: Sink[Task, A] = stdOutLines.contramap(_.toString)

  def queueProducers[A](nrOfJobs: Int, queue: BoundedQueue[A], producer: Process[Task, Throwable \/ A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(producer.drainW(printlnStr).to(queue.enqueue)).take(nrOfJobs))

  def dequeueWorkers[A](nrOfJobs: Int, queue: BoundedQueue[A], consumer: Sink[Task, A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(queue.dequeue.to(consumer)).take(nrOfJobs))

  val fixSizeQueue: BoundedQueue[String] = async.boundedQueue[String](1000)
  val producers: Process[Task, Unit] = queueProducers(1, fixSizeQueue, sqsProducer(scheduler)(config))
  val consumers: Process[Task, Unit] = dequeueWorkers(4, fixSizeQueue, printlnStr)

  // We would log any errors here... but that should never happen :\
  //
  producers.run.runAsync(_ ⇒ ())
  consumers.run.runAsync(_ ⇒ ())
  fixSizeQueue.size.discrete.map(s ⇒ s"Size: $s").to(printlnStr).run.runAsync(_ ⇒ ())
  //

  println("waiting to stop")

  StdIn.readLine()

  scheduler.shutdownNow()
}

trait SqsProducer {
  // Using Process.state to query SQS using an exponential backoff.
  //
  def sqsProducer(scheduler: ScheduledExecutorService): Reader[AmazonZConfig, Process[Task, Throwable \/ String]] = Reader(config ⇒
    Process.state(1).flatMap[Task, Throwable \/ String] {
      case (get, set) ⇒
        Process.eval(
          for {
            _ ← Task.schedule((), get.millis)(scheduler)
            text ← Task.async[String](asyncFunction(get * 2, _)).attempt
            next = if (math.random > 0.10) math.min(get * 2, 30000) else 1
            _ ← set(next)
          } yield text
        )
    }
  )
  //

  private[this] def asyncFunction(i: Int, f: Throwable \/ String ⇒ Unit): Unit =
    if (math.random > 0.50) f(\/-(s"No error $i")) else f(-\/(new Throwable(s"An error $i")))

  implicit class TaskSchedulingImplicit(future: Task.type) {
    /** Create a `Future` that will evaluate `a` after at least the given delay. */
    def schedule[A](a: ⇒ A, delay: Duration)(implicit pool: ScheduledExecutorService): Task[A] =
      Task.async { cb ⇒
        val _ = pool.schedule(new Callable[Unit] {
          def call: Unit = cb(\/-(a))
        }, delay.toMillis, TimeUnit.MILLISECONDS)
      }
  }
}
