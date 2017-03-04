package com.s3dropbox.lambda

import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord

class EventRecords(eventRecords: List[S3EventNotificationRecord]) {

  def stringified: List[String] = {
    eventRecords
      .map { rec =>
        s"""Event Name: ${rec.getEventName},
           | Event Source: ${rec.getEventSource},
           | Bucket: ${rec.getS3.getBucket.getName},
           | Object: ${rec.getS3.getObject.getKey},
           | S3 Schema Version: ${rec.getS3.getS3SchemaVersion},
           | Configuration ID: ${rec.getS3.getConfigurationId}""".stripMargin
      }
  }
}
