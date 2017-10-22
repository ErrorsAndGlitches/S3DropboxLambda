package com.s3dropbox.lambda

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.file.attribute.FileTime
import java.util.zip.{ZipEntry, ZipInputStream}

import com.s3dropbox.lambda.ZipFileIterator.{END_OF_FILE, ONE_MB, PAGE_SIZE, ZipFileEntry}

/**
  * ZipFileIterator iterates over the files in a zip file.
  */
class ZipFileIterator(istream: InputStream) extends Iterator[ZipFileEntry] {

  private val zis: ZipInputStream = new ZipInputStream(istream)
  private var zentryOpt: Option[ZipEntry] = None

  override def hasNext: Boolean = {
    zentryOpt = Option(zis.getNextEntry)
    zentryOpt.isDefined
  }

  override def next(): ZipFileEntry = {
    val zentry: ZipEntry = zentryOpt.getOrElse(throw new Exception("Calling next() when there are no ZipEntry's to process"))

    // sometimes the size cannot be determined, in which case, the buffer is read until no more data is read
    val bosSize: Int = if (zentry.getSize > 0) zentry.getSize.toInt else ONE_MB
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(bosSize)

    // even though we may know the exact size of the file, ZipInputStream.read() may return a value < the known file size
    val readBuffer: Array[Byte] = new Array[Byte](PAGE_SIZE)
    Iterator
      .continually(zis.read(readBuffer, 0, PAGE_SIZE))
      .takeWhile((bytesRead: Int) => bytesRead != END_OF_FILE)
      .foreach((bytesRead: Int) => bos.write(readBuffer, 0, bytesRead))

    ZipFileEntry(zentry.getName, bos.toByteArray, zentry.getLastModifiedTime)
  }

  def close(): Unit = {
    zis.closeEntry()
    zis.close()
  }
}

object ZipFileIterator {

  case class ZipFileEntry(filename: String, data: Array[Byte], fileTime: FileTime)

  private[ZipFileIterator] val ONE_MB: Int = 1024 * 1024
  private[ZipFileIterator] val END_OF_FILE: Int = -1
  private[ZipFileIterator] val PAGE_SIZE: Int = 1024
}
