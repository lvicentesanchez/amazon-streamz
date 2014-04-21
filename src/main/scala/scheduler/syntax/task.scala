package scheduler
package syntax

import java.util.concurrent.{ Callable, ScheduledExecutorService, TimeUnit }
import scala.concurrent.duration._
import scalaz.concurrent.Task
import scheduler.task._

object task {
  implicit class TaskOps(val self: Task.type) extends AnyVal {
    def schedule[A](value: ⇒ A, delay: Duration)(implicit pool: ScheduledExecutorService): Task[A] =
      Scheduler[Task].schedule(value, delay)(pool)
  }
}
