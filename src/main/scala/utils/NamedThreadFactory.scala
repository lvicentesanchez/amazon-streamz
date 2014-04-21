package utils

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(name: String) extends ThreadFactory {
  val threadNr = new AtomicInteger()
  val backingThreadFactory = Executors.defaultThreadFactory()

  def newThread(r: Runnable) = {
    val thread = backingThreadFactory.newThread(r)
    thread.setName(name + "-" + threadNr.incrementAndGet())
    thread
  }
}
