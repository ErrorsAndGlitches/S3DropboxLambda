package com.s3dropbox.lambda

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.joda.time.DateTime

/**
  * ZipFileIteratorSpec
  */
class ZipFileIteratorSpec extends UnitSpec {

  val LAST_MODIFIED_TIME: FileTime = FileTime.from(
    new DateTime(2001, 12, 19, 0, 0).getMillis,
    TimeUnit.MILLISECONDS
  )

  describe("When given a valid zip file with more than one compressed file") {
    it("should iterate over the compressed files and provide their file name and contents") {
      val entries: Array[(String, Array[Byte])] =
        (1 to 5)
          .map((fileNum: Int) => {
            (s"file_$fileNum", s"This is the contents of file $fileNum".getBytes)
          })
          .toArray
      runZipFileTest(entries)
    }
  }

  describe("when given a zip file containing a single large 1MB file") {
    it("should decompress the single large file") {
      val entries: Array[(String, Array[Byte])] = Array(
        ("some_file", (1 to 1024 * 1024).map(_.toByte).toArray)
      )
      runZipFileTest(entries)
    }
  }

  def runZipFileTest(entries: Array[(String, Array[Byte])]): Unit = {
    var index: Int = 0
    val zipFileIter: ZipFileIterator = new ZipFileIterator(new FileInputStream(zipFile(entries)))
    zipFileIter.foreach((zipFileEntry: ZipFileEntry) => {
      assert(zipFileEntry.filename == entries(index)._1)
      assert(zipFileEntry.data sameElements entries(index)._2)
      assert(zipFileEntry.fileTime == LAST_MODIFIED_TIME)
      index += 1
    })
    zipFileIter.close()
  }

  def zipFile(entries: Array[(String, Array[Byte])]): File = {
    val temp: File = File.createTempFile("temp-zip-file", ".zip")
    temp.deleteOnExit()

    val zos: ZipOutputStream = new ZipOutputStream(new FileOutputStream(temp))
    entries.foreach((entry: (String, Array[Byte])) => {
      val filename: String = entry._1
      val contents: Array[Byte] = entry._2

      val zentry: ZipEntry = new ZipEntry(filename)
      zentry.setLastModifiedTime(LAST_MODIFIED_TIME)
      zos.putNextEntry(zentry)
      zos.write(contents)
      zos.closeEntry()
    })
    zos.close()

    temp
  }
}
