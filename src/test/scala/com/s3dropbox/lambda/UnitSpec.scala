package com.s3dropbox.lambda

import org.scalatest.FunSpec

/**
  * The base Spec that all specifications extend.
  */
class UnitSpec extends FunSpec {
  def resourceFile(filename: String): String = {
    getClass.getResource(s"/$filename").getPath
  }
}
