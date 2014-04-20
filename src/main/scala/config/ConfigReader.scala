package config

import com.amazonaws.auth.{ AWSCredentials, BasicAWSCredentials }
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.sqs.{ AmazonSQSAsync, AmazonSQSAsyncClient }
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import scalaz.Reader

trait ConfigReader {
  def configReader: Reader[Config, AmazonZConfig] =
    for {
      sqsConfig ← sqsConfigReader
    } yield AmazonZConfig(sqsConfig)

  private def sqsConfigReader: Reader[Config, SQSConfig] =
    for {
      accountNr ← Reader[Config, String](config ⇒ config.getString("sqs.accountNr"))
      accessKey ← Reader[Config, String](config ⇒ config.getString("sqs.accessKey"))
      secretKey ← Reader[Config, String](config ⇒ config.getString("sqs.secretKey"))
      regionURL ← Reader[Config, String](config ⇒ config.getString("sqs.sqsRegion"))
      queueName ← Reader[Config, String](config ⇒ config.getString("sqs.queueName"))
      credentials ← Reader[Config, AWSCredentials](_ ⇒
        new BasicAWSCredentials(accessKey, secretKey)
      )
      asyncClient ← Reader[Config, AmazonSQSAsync](_ ⇒
        new AmazonSQSAsyncClient(credentials) {
          this.setRegion(RegionUtils.getRegionByEndpoint(regionURL))
        }
      )
    } yield SQSConfig(accountNr, credentials, regionURL, 200, queueName, asyncClient)
}
