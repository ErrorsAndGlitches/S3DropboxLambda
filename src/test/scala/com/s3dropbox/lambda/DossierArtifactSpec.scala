package com.s3dropbox.lambda

import com.s3dropbox.lambda.DossierArtifactSpec.EmptyByteArray
import com.s3dropbox.lambda.ZipFileIterator.ZipFileEntry
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar

class DossierArtifactSpec extends AnyFunSpec with MockitoSugar {

  describe("when given a zip file with PDFs and a manifest file") {
    it("should unpack those zip files and manifest file") {
      testDossierArtifact.dossierZipFileContents.manifest shouldBe Manifest(
        List(FileState("file-0.pdf", "file-0-md5sum"), FileState("folder-1/file-1.pdf", "file-1-md5sum"))
      )
    }
  }

  describe("when the manifest file is missing") {
    it("should throw an exception") {
      intercept[IllegalStateException] {
        DossierArtifact(mockZipFileIterator(List(
          ("file-0.pdf", EmptyByteArray),
          ("file-1.pdf", EmptyByteArray)
        )))
          .dossierZipFileContents
          .manifest
      }
    }
  }

  describe("when the new manifest is identical to the current manifest") {
    it("should provide the expected differences") {
      val oldManifest: Manifest = Manifest(
        List(FileState("file-0.pdf", "file-0-md5sum"), FileState("folder-1/file-1.pdf", "file-1-md5sum"))
      )
      testDossierArtifact.filenamesToRemove(oldManifest).isEmpty should be
      testDossierArtifact.zipFileEntriesToUpdate(oldManifest).isEmpty should be
    }
  }

  describe("when the new manifest is different from the current manifest") {
    it("should provide the expected differences") {
      val oldManifest: Manifest = Manifest(
        List(FileState("file-0.pdf", "file-0-old-md5sum"), FileState("file-2.pdf", "file-2-md5sum"))
      )
      testDossierArtifact.filenamesToRemove(oldManifest) shouldBe List("file-2.pdf")
      testDossierArtifact.zipFileEntriesToUpdate(oldManifest) shouldBe List(
        ZipFileEntry("file-0.pdf", EmptyByteArray),
        ZipFileEntry("folder-1/file-1.pdf", EmptyByteArray)
      )
    }
  }

  describe("when a dossier artifact is closed") {
    it("should close the underlying iterator") {
      val mockIterator = mock[ZipFileIterator]
      DossierArtifact(mockIterator).close()
      verify(mockIterator).close()
    }
  }

  private def testDossierArtifact: DossierArtifact = DossierArtifact(mockZipFileIterator(List(
    ("file-0.pdf", EmptyByteArray),
    ("folder-1/", EmptyByteArray),
    ("folder-1/file-1.pdf", EmptyByteArray),
    (".manifest",
      """{ "fileStates":[
        |{ "filename": "file-0.pdf", "md5sum": "file-0-md5sum" },
        |{ "filename": "folder-1/file-1.pdf", "md5sum": "file-1-md5sum" }
        |]}""".stripMargin('|').getBytes)
  )))

  private def mockZipFileIterator(entries: List[(String, Array[Byte])]): ZipFileIterator = {
    val mockIterator: ZipFileIterator = mock[ZipFileIterator]
    when(mockIterator.foreach(any()))
      .thenAnswer((invocation: InvocationOnMock) => {
        val func: ZipFileEntry => Unit = invocation.getArgument(0)
        entries
          .map(entry => ZipFileEntry(entry._1, entry._2))
          .foreach(func)
      })
    mockIterator
  }
}

private object DossierArtifactSpec {
  val EmptyByteArray: Array[Byte] = Array[Byte]()
}
