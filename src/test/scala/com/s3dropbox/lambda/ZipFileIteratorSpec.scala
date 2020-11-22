package com.s3dropbox.lambda

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.scalatest.funspec.AnyFunSpec

/**
  * ZipFileIteratorSpec
  */
class ZipFileIteratorSpec extends AnyFunSpec with ResourceSpec {

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

  describe("when given a zip file containing 10 PDF files and 10 LaTeX files") {
    it("should decompress the 20 files") {
      val zipFileIter: ZipFileIterator = new ZipFileIterator(resourceFileStream("test_files.zip"))

      assert(
        zipFileIter.foldLeft(0)((numItems: Int, _: ZipFileEntry) => numItems + 1) == 20
      )
    }
  }

  def runZipFileTest(entries: Array[(String, Array[Byte])]): Unit = {
    var index: Int = 0
    val zipFileIter: ZipFileIterator = new ZipFileIterator(new FileInputStream(zipFile(entries)))
    zipFileIter.foreach((zipFileEntry: ZipFileEntry) => {
      assert(zipFileEntry.filename == entries(index)._1)
      assert(zipFileEntry.data sameElements entries(index)._2)
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
      zos.putNextEntry(zentry)
      zos.write(contents)
      zos.closeEntry()
    })
    zos.close()

    temp
  }
}
