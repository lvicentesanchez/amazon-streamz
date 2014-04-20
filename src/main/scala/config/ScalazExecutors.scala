package config

import java.util.concurrent.ExecutorService

case class ScalazExecutors(async: ExecutorService, block: ExecutorService)