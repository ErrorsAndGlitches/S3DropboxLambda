package com.s3dropbox.lambda

import scopt.OptionParser

case class DossierCliOpts(
  dbxCredBucket: String = "", dbxCredKey: String = "", kmsId: String = "",
  dossierBucket: String = "", dossierKey: String = "", awsRegion: String = "us-west-2"
)

object DossierCliOpts {
  def optionParser: OptionParser[DossierCliOpts] = new OptionParser[DossierCliOpts]("DBX Dossier") {
    opt[String]("dbxCredBucket")
      .action((bucket, opts) => opts.copy(dbxCredBucket = bucket))
      .required()
      .text("S3 bucket to store Dropbox short term credentials")
    opt[String]("dbxCredKey")
      .action((key, opts) => opts.copy(dbxCredKey = key))
      .required()
      .text("S3 key to store Dropbox short term credentials")
    opt[String]("kmsId")
      .action((kmsId, opts) => opts.copy(kmsId = kmsId))
      .required()
      .text("KMS ID used to encrypted Dropbox credentials in S3")
    opt[String]("dossierBucket")
      .action((bucket, opts) => opts.copy(dossierBucket = bucket))
      .required()
      .text("S3 bucket of the Dossier zip file")
    opt[String]("dossierKey")
      .action((key, opts) => opts.copy(dossierKey = key))
      .required()
      .text("S3 key of the Dossier zip file")
    opt[String]("awsRegion")
      .action((region, opts) => opts.copy(awsRegion = region))
      .text("Set the AWS client region")
    help("help")
  }
}
