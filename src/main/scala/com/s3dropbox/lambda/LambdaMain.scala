package com.s3dropbox.lambda

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.collection.JavaConverters._

final class LambdaMain extends RequestHandler[S3Event, Unit] {

  override def handleRequest(event: S3Event, context: Context): Unit = {
    val stringifiedRecs: List[String] = new EventRecords(event.getRecords.asScala.toList).stringified

    println(
      s"""${context.getFunctionName} handling the following event:
         |${stringifiedRecs.map { rec => s"\t$rec"} }
       """.stripMargin
    )
  }
}
