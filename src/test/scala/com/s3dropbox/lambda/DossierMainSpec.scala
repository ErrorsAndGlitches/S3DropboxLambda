package com.s3dropbox.lambda

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{S3Object, S3ObjectInputStream}
import com.s3dropbox.lambda.DossierMainSpec.{TestDossierBucket, TestDossierKey}
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable.ArrayBuffer

class DossierMainSpec extends AnyFunSpec with MockitoSugar with Matchers with ResourceSpec {

  describe("when the dossier artifacts need to be updated") {
    it("should call the Dropbox APIs to update the artifacts") {
      val uploadedFiles: ArrayBuffer[String] = ArrayBuffer[String]()
      val deletedFiles: ArrayBuffer[String] = ArrayBuffer[String]()
      var finalManifest: Manifest = null

      val dbxFiles = mockDbxFiles
      when(dbxFiles.uploadPdf(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          uploadedFiles.append(invocation.getArgument(0, classOf[ZipFileEntry]).filename)
        }
      })
      when(dbxFiles.deletePdf(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          deletedFiles.append(invocation.getArgument(0, classOf[String]))
        }
      })
      when(dbxFiles.uploadManifest(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          finalManifest = invocation.getArgument(0, classOf[Manifest])
        }
      })

      DossierMain(mockS3, dbxFiles, mockManifest).update(TestDossierBucket, TestDossierKey)

      uploadedFiles shouldBe Array(
        "file_1.pdf", "file_2.pdf", "file_3.pdf", "file_4.pdf", "file_5.pdf", "file_6.pdf",
        "file_7.pdf", "file_8.pdf", "file_9.pdf"
      )
      deletedFiles shouldBe Array("file_should_delete.tex")
      finalManifest shouldBe Manifest(FileStates(List(
        FileState("file_9.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_8.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_7.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_6.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_5.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_4.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_3.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_2.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_1.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        FileState("file_0.tex", "1B2M2Y8AsgTpgAmY7PhCfg==")
      )))
    }
  }

  private def mockS3: AmazonS3 = {
    val s3Object: S3Object = mock[S3Object]
    when(s3Object.getObjectContent).thenReturn(new S3ObjectInputStream(
      resourceFileStream("test_files.zip"),
      null
    ))
    val s3: AmazonS3 = mock[AmazonS3]
    when(s3.getObject(anyString(), anyString())).thenReturn(s3Object)
    s3
  }

  private def mockDbxFiles: DbxFiles = {
    val dbxFiles: DbxFiles = mock[DbxFiles]
    dbxFiles
  }

  private def mockManifest: Manifest = Manifest(FileStates(List(
    FileState("file_0.tex", "1B2M2Y8AsgTpgAmY7PhCfg=="), // no update
    FileState("file_1.tex", "000000000000000000000000"), // should update
    FileState("file_should_delete.tex", "000000000000000000000000"),
  )))
}

object DossierMainSpec {
  val TestDossierBucket = "DossierBucket"
  val TestDossierKey = "DossierKey"
}
