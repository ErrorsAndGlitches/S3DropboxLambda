package com.s3dropbox.lambda

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DossierCliOptsSpec extends AnyFunSpec with Matchers {
  describe("when no options are given") {
    it("should not return a DossierCliOpts") {
      DossierCliOpts.optionParser.parse(Seq[String](), DossierCliOpts()) shouldBe None
    }
  }

  describe("when all required options are specified") {
    it("should return a valid DossierCliOpts") {
      DossierCliOpts.optionParser.parse(Seq[String](
        "--dbxCredBucket", "dbxCredBucket",
        "--dbxCredKey", "dbxCredKey",
        "--kmsId", "kmsId",
        "--dossierBucket", "dossierBucket",
        "--dossierKey", "dossierKey"
      ), DossierCliOpts()) shouldBe Some(DossierCliOpts(
        "dbxCredBucket",
        "dbxCredKey",
        "kmsId",
        "dossierBucket",
        "dossierKey",
        "us-west-2"
      ))
    }
  }

  describe("when all options are specified") {
    it("should return a valid DossierCliOpts") {
      DossierCliOpts.optionParser.parse(Seq[String](
        "--dbxCredBucket", "dbxCredBucket",
        "--dbxCredKey", "dbxCredKey",
        "--kmsId", "kmsId",
        "--dossierBucket", "dossierBucket",
        "--dossierKey", "dossierKey",
        "--awsRegion", "us-east-1"
      ), DossierCliOpts()) shouldBe Some(DossierCliOpts(
        "dbxCredBucket",
        "dbxCredKey",
        "kmsId",
        "dossierBucket",
        "dossierKey",
        "us-east-1"
      ))
    }
  }
}
