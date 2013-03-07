package net.koofr.zipstream

private[zipstream] object internal {
  import scala.concurrent.ExecutionContext
  import java.util.concurrent.Executors
  import java.util.concurrent.ThreadFactory
  import java.util.concurrent.atomic.AtomicInteger

  implicit lazy val defaultExecutionContext: ExecutionContext = {
    val numberOfThreads = try {
      com.typesafe.config.ConfigFactory.load().getInt("zipstream-threadpool-size")
    } catch { case e: com.typesafe.config.ConfigException.Missing =>
      Runtime.getRuntime.availableProcessors
    }
    
    val threadFactory = new ThreadFactory {
      val threadNo = new AtomicInteger()
      val backingThreadFactory = Executors.defaultThreadFactory()
      def newThread(r: Runnable) = {
        val thread = backingThreadFactory.newThread(r)
        thread.setName("zipstream-execution-context-" + threadNo.incrementAndGet())
        thread
      }
    }

    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberOfThreads, threadFactory))
  }
}
