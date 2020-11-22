package com.s3dropbox.lambda

import java.io.InputStream

/**
  * A trait to add a convenience method for getting test resources.
  */
trait ResourceSpec {
  def resourceFileStream(filename: String): InputStream = {
    getClass.getResourceAsStream(s"/$filename")
  }
}
