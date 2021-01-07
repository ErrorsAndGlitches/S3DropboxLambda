package com.s3dropbox.lambda

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{S3Object, S3ObjectInputStream}
import com.s3dropbox.lambda.DossierMainSpec.{TestDossierBucket, TestDossierKey}
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
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

      val dbxFiles = mock[DbxFiles]
      when(dbxFiles.uploadPdfs(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          invocation
            .getArgument(0, classOf[List[ZipFileEntry]])
            .map(_.filename)
            .foreach(uploadedFiles.append(_))
        }
      })
      when(dbxFiles.deletePdfs(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          deletedFiles ++= invocation.getArgument(0, classOf[List[String]])
        }
      })
      when(dbxFiles.uploadManifest(any())).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          finalManifest = invocation.getArgument(0, classOf[Manifest])
        }
      })

      DossierMain(mockS3, dbxFiles, currentManifest).update(TestDossierBucket, TestDossierKey)

      uploadedFiles shouldBe Array(
        "file-1.pdf",
        "folder-2/file-2.pdf",
        "folder-3/sub-folder-3/file-3.pdf"
      )
      deletedFiles shouldBe Array("folder-4/sub-folder-4/file-4.pdf")
      finalManifest shouldBe Manifest(List(
        FileState("file-0.pdf", "file-0-md5sum"),
        FileState("file-1.pdf", "file-1-md5sum"),
        FileState("folder-2/file-2.pdf", "file-2-md5sum"),
        FileState("folder-3/sub-folder-3/file-3.pdf", "file-3-md5sum")
      ))
    }
  }

  private def mockS3: AmazonS3 = {
    val s3Object: S3Object = mock[S3Object]
    when(s3Object.getObjectContent).thenReturn(new S3ObjectInputStream(
      resourceFileStream("test-files.zip"),
      null
    ))
    val s3: AmazonS3 = mock[AmazonS3]
    when(s3.getObject(anyString(), anyString())).thenReturn(s3Object)
    s3
  }

  private def currentManifest: Manifest = Manifest(List(
    FileState("file-0.pdf", "file-0-md5sum"),
    FileState("file-1.pdf", "file-1-old-md5sum"),
    FileState("folder-2/file-2.pdf", "file-2-old-md5sum"),
    FileState("folder-4/sub-folder-4/file-4.pdf", "file-4-md5sum")
  ))
}

object DossierMainSpec {
  val TestDossierBucket = "DossierBucket"
  val TestDossierKey = "DossierKey"
}
