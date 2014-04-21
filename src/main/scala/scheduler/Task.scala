package scheduler

import java.util.concurrent.{ Callable, ScheduledExecutorService, TimeUnit }
import scala.concurrent.duration._
import scalaz.\/
import scalaz.concurrent.Task

trait TaskSchedulerInstance {
  implicit val taskInstance: Scheduler[Task] = new Scheduler[Task] {
    def schedule[A](value: ⇒ A, delay: Duration)(implicit pool: ScheduledExecutorService): Task[A] =
      Task.async { cb ⇒
        val _ = pool.schedule(new Callable[Unit] {
          def call: Unit = cb(\/.right(value))
        }, delay.toMillis, TimeUnit.MILLISECONDS)
      }
  }
}

object task extends TaskSchedulerInstance
