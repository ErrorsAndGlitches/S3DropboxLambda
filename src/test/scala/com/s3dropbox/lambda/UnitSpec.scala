package com.s3dropbox.lambda

import org.scalatest.FunSpec
import org.scalatest.mockito.MockitoSugar

/**
  * The base Spec that all specifications extend.
  */
class UnitSpec extends FunSpec with MockitoSugar {
  def resourceFile(filename: String): String = {
    getClass.getResource(s"/$filename").getPath
  }
}
