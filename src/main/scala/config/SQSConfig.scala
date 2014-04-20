package config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsync

case class SQSConfig(accountNr: String, credentials: AWSCredentials, regionURL: String, groupSize: Int, queue: String, client: AmazonSQSAsync)
