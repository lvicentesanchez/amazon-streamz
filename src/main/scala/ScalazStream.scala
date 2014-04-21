import config.{ AmazonZConfig, ConfigReader }
import com.amazonaws.services.sqs.model.Message
import com.typesafe.config.ConfigFactory
import java.util.concurrent.{ Executors, ScheduledExecutorService }
import producers.aws
import scala.io.StdIn
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.stream.{ Process, Sink }
import scalaz.stream.async
import scalaz.stream.async.mutable._
import scalaz.stream.merge._
import scalaz.stream.processes._
import utils._

object ScalazStream extends App with ConfigReader {
  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("task-scheduler"))

  val config: AmazonZConfig = configReader(ConfigFactory.load())

  def printlnStr[A]: Sink[Task, A] = stdOutLines.contramap(_.toString)

  def queueProducers[A](nrOfJobs: Int, queue: BoundedQueue[A], producer: Process[Task, Throwable \/ A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(producer.drainW(printlnStr).to(queue.enqueue)).take(nrOfJobs))

  def dequeueWorkers[A](nrOfJobs: Int, queue: BoundedQueue[A], consumer: Sink[Task, A]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(queue.dequeue.to(consumer)).take(nrOfJobs))

  val fixSizeQueue: BoundedQueue[List[Message]] = async.boundedQueue[List[Message]](1000)
  val producers: Process[Task, Unit] = queueProducers(1, fixSizeQueue, aws.sqsProducer(scheduler)(config))
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
  config.sqs.client.shutdown()
}

