package scheduler

import java.util.concurrent.ScheduledExecutorService
import scala.concurrent.duration._

trait Scheduler[F[_]] { self ⇒
  def schedule[A](value: ⇒ A, delay: Duration)(implicit pool: ScheduledExecutorService): F[A]
}

object Scheduler {
  @inline def apply[F[_]](implicit F: Scheduler[F]): Scheduler[F] = F
}

