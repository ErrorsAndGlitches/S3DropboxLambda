package com.s3dropbox.lambda

import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.JsonMethods

import java.io.ByteArrayInputStream

/**
  * Manifest contains information about the target Dropbox App folder.
  */
case class Manifest(fileStates: List[FileState]) {
  def filenamesToRemove(oldManifest: Manifest): List[String] = oldManifest.filenames.diff(filenames)

  def filesToUpdate(oldManifest: Manifest): List[FileState] = fileStates.diff(oldManifest.fileStates)

  private def filenames: List[String] = fileStates.map(_.filename)
}

object Manifest {
  implicit val formats: Formats = org.json4s.DefaultFormats + NoTypeHints

  def apply(): Manifest = Manifest(List[FileState]())

  def apply(data: Array[Byte]): Manifest = JsonMethods.parse(new ByteArrayInputStream(data)).extract[Manifest]
}

case class FileState(filename: String, md5sum: String)
