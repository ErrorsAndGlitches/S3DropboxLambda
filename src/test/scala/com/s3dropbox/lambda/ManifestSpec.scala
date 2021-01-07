package com.s3dropbox.lambda

import org.json4s.JsonAST.{JObject, JString}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.json4s.{Formats, JsonAST}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class ManifestSpec extends AnyFunSpec {

  describe("when the new manifest is identical to the current manifest") {
    it("should have no updates and no files to remove") {
      val newManifest = testManifest
      val oldManifest = testManifest

      newManifest.filesToUpdate(oldManifest).isEmpty should be
      newManifest.filenamesToRemove(oldManifest).isEmpty should be
    }
  }

  describe("when the new manifest has one new file compared to the old manifest") {
    it("should have one update and no files to remove") {
      val newManifest = testManifest((0 to 3).toList)
      val oldManifest = testManifest((0 to 2).toList)

      newManifest.filesToUpdate(oldManifest) shouldBe List(FileState("file3", "md5sum-file3"))
      newManifest.filenamesToRemove(oldManifest).isEmpty should be
    }
  }

  describe("when the new manifest has one less file compared to the old manifest") {
    it("should have no updates and one file to remove") {
      val newManifest = testManifest((0 to 2).toList)
      val oldManifest = testManifest((0 to 3).toList)

      newManifest.filesToUpdate(oldManifest).isEmpty should be
      newManifest.filenamesToRemove(oldManifest) shouldBe List("file3")
    }
  }

  describe("when the new manifest has the same files as the old manifest, but with different contents") {
    it("should have updates no files to remove") {
      val newManifest = Manifest((0 to 3).map(index => FileState(filename(index), s"new-md5sum-file$index")).toList)
      val oldManifest = testManifest((0 to 3).toList)

      newManifest.filesToUpdate(oldManifest) shouldBe List(
        FileState("file0", "new-md5sum-file0"),
        FileState("file1", "new-md5sum-file1"),
        FileState("file2", "new-md5sum-file2"),
        FileState("file3", "new-md5sum-file3")
      )
      newManifest.filenamesToRemove(oldManifest).isEmpty should be
    }
  }

  describe("when two identical manifest objects are compared") {
    it("should indicate that they are equal") {
      val manifestOne: Manifest = testManifest
      val manifestTwo: Manifest = testManifest

      assert(manifestOne == manifestTwo)
    }
  }

  describe("when a manifest object is serialized") {
    it("should be serialized to the expected JSON string") {
      implicit val formats: Formats = DossierConfig.JsonFormat
      val manifest: Manifest = testManifest(List(0))

      assert(
        Serialization.write(manifest) == write(JObject((
          "fileStates",
          JsonAST.JArray(List(
            JObject(List(
              ("filename", JString("file0")),
              ("md5sum", JString("md5sum-file0"))
            ))
          ))
        )))
      )
    }
  }

  private def testManifest: Manifest = testManifest((0 to 3).toList)

  private def testManifest(indices: List[Int]): Manifest = Manifest(indices
    .map((index: Int) => filename(index))
    .map((filename: String) => FileState(filename, s"md5sum-$filename"))
  )

  private def filename(index: Int): String = s"file$index"
}
