import config.{ AmazonZConfig, ConfigReader }
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.model.Message
import com.typesafe.config.ConfigFactory
import java.util.concurrent.{ Executors, ScheduledExecutorService, TimeUnit }
import amazon.sqs
import scala.io.StdIn
import scalaz.\/
import scalaz.concurrent.Task
import scalaz.stream.{ Channel, Process, Sink }
import scalaz.stream.async
import scalaz.stream.async.mutable._
import scalaz.stream.merge._
import scalaz.stream.processes._
import utils._

object ScalazStream extends App with ConfigReader {
  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("task-scheduler"))

  val config: AmazonZConfig = configReader(ConfigFactory.load())

  def printlnStr[A]: Sink[Task, A] = stdOutLines.contramap(_.toString)

  def queueProducers[A](nrOfJobs: Int, queue: BoundedQueue[A], producer: Process[Task, Throwable \/ A], logger: Sink[Task, Throwable]): Process[Task, Unit] =
    mergeN(nrOfJobs)(Process.constant(producer.drainW(logger).to(queue.enqueue)).take(nrOfJobs))

  def queueConsumers[A, B](nrOfJobs: Int, queue: BoundedQueue[A], channel: Channel[Task, A, Throwable \/ B], logger: Sink[Task, Throwable]): Process[Task, B] =
    mergeN(nrOfJobs)(Process.constant(queue.dequeue.through(channel).drainW(logger)).take(nrOfJobs))

  val fixSizeQueue: BoundedQueue[List[Message]] = async.boundedQueue[List[Message]](1000)
  val producers: Process[Task, Unit] = queueProducers(4, fixSizeQueue, sqs.dequeue(scheduler)(config), printlnStr)
  val consumers: Process[Task, Unit] = queueConsumers(4, fixSizeQueue, sqs.destroy(config), printlnStr).to(printlnStr)

  // We would log any errors here... but that should never happen :\
  //
  producers.run.runAsync(_ ⇒ ())
  consumers.run.runAsync(_ ⇒ ())
  fixSizeQueue.size.discrete.map(s ⇒ s"Size: $s").to(printlnStr).run.runAsync(_ ⇒ ())
  //

  StdIn.readLine()

  scheduler.shutdownNow()
  scheduler.awaitTermination(30, TimeUnit.SECONDS)
  config.sqs.client.asInstanceOf[AmazonSQSAsyncClient].getExecutorService.shutdownNow()
  config.sqs.client.asInstanceOf[AmazonSQSAsyncClient].getExecutorService.awaitTermination(30, TimeUnit.SECONDS)
  config.sqs.client.shutdown()
}

