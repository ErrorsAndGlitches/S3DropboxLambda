package com.s3dropbox.lambda

import java.security.MessageDigest
import java.util.Base64
import com.s3dropbox.lambda.Manifest.digest

/**
  * Manifest contains information about the target Dropbox App folder.
  */
case class Manifest(fileStates: FileStates) {

  def requiresUpdate(texFilename: String, texFileBody: Array[Byte]): Boolean = {
    fileStates.requiresUpdate(FileState(texFilename, digest(texFileBody)))
  }

  def updateFileState(texFilename: String, texFileBody: Array[Byte]): Manifest = {
    Manifest(
      fileStates.update(FileState(texFilename, digest(texFileBody)))
    )
  }

  def removeFileState(texFilename: String): Manifest = {
    Manifest(
      fileStates.remove(texFilename)
    )
  }
}

object Manifest {
  private val Md5Algorithm: String = "MD5"

  def apply(): Manifest = Manifest(FileStates(List[FileState]()))

  def digest(texFileBody: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(
      MessageDigest
        .getInstance(Manifest.Md5Algorithm)
        .digest(texFileBody)
    )
  }
}

case class FileStates(fileStates: List[FileState]) {

  def requiresUpdate(newFileState: FileState): Boolean = {
    fileStates
      .find((state: FileState) => state.filename == newFileState.filename)
      .forall((state: FileState) => state.md5sum != newFileState.md5sum)
  }

  def update(newFileState: FileState): FileStates = {
    FileStates(
      newFileState ::
        fileStates.filter((fState: FileState) => fState.filename != newFileState.filename)
    )
  }

  def remove(texFileName: String): FileStates = {
    FileStates(
      fileStates.filter((fState: FileState) => fState.filename != texFileName)
    )
  }

  override def equals(o: scala.Any): Boolean = {
    if (!o.isInstanceOf[FileStates]) {
      false
    }
    else {
      val otherFileStates: FileStates = o.asInstanceOf[FileStates]
      otherFileStates.sortedFileStates == sortedFileStates
    }
  }

  private def sortedFileStates: List[FileState] = fileStates.sortBy(_.filename)
}

case class FileState(filename: String, md5sum: String)
