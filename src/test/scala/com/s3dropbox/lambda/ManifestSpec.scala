package com.s3dropbox.lambda

import java.nio.charset.StandardCharsets

import com.s3dropbox.lambda.Manifest.digest
import com.s3dropbox.lambda.ManifestSpec.FileBodyBytes
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import org.json4s.{Formats, JsonAST}

class ManifestSpec extends UnitSpec {

  describe("when the manifest contains the file and the data is the same") {
    it("should indicate that no update is required") {
      assert(
        !testManifest.requiresUpdate(filename(1), FileBodyBytes)
      )
    }
  }

  describe("when the manifest contains the file and the data is not the same") {
    it("should indicate that an update is required") {
      assert(
        testManifest.requiresUpdate(filename(1), "different file body".getBytes(StandardCharsets.UTF_8))
      )
    }
  }

  describe("when the manifest does not contain the file") {
    it("should indicate that an update is required") {
      assert(
        testManifest.requiresUpdate(filename(9), FileBodyBytes)
      )
    }
  }

  describe("when a new file is added to an empty manifest") {
    it("should return a new manifest with the file") {
      assert(
        Manifest(FileStates(List())).updateFileState(filename(9), FileBodyBytes) == testManifest(List(9))
      )
    }
  }

  describe("when the manifest is updated with a file that doesn't already exist") {
    it("should return a new manifest with the file") {
      assert(
        testManifest.updateFileState(filename(9), FileBodyBytes) == testManifest(List(0, 1, 2, 3, 9))
      )
    }
  }

  describe("when the manifest is updated with a file that already exists") {
    it("should return a new manifest with the file") {
      val newBodyBytes: Array[Byte] = "new file contents".getBytes(StandardCharsets.UTF_8)

      assert(
        testManifest.updateFileState(filename(2), newBodyBytes) == Manifest(FileStates(List(
          FileState(filename(0), digest(FileBodyBytes)),
          FileState(filename(1), digest(FileBodyBytes)),
          FileState(filename(2), digest(newBodyBytes)),
          FileState(filename(3), digest(FileBodyBytes)),
        )))
      )
    }
  }

  describe("when two empty manifest objects are compared") {
    it("should indicate that they are equal") {
      val manifestOne: Manifest = Manifest(FileStates(List[FileState]()))
      val manifestTwo: Manifest = Manifest(FileStates(List[FileState]()))

      assert(manifestOne == manifestTwo)
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
      implicit val formats: Formats = LambdaMain.JsonFormat
      val manifest: Manifest = testManifest(List(0))

      assert(
        Serialization.write(manifest).toString == write(JObject((
          "fileStates",
          JObject((
            "fileStates",
            JsonAST.JArray(List(
                JObject(List(
                    ("filename", JString("file0")),
                    ("md5sum", JString("xj7O6ohpJ/eKKU0J6t/WTA=="))
                  )
                )
              )
            )
          ))
        )))
      )
    }
  }

  private def testManifest: Manifest = testManifest((0 to 3).toList)

  private def testManifest(indices: List[Int]): Manifest = Manifest(FileStates(
    indices
      .map((index: Int) => filename(index))
      .map((filename: String) => FileState(filename, ManifestSpec.MD5Sum))
  ))

  private def filename(index: Int): String = s"file$index"
}

private object ManifestSpec {
  val FileBodyBytes: Array[Byte] = "file body".getBytes(StandardCharsets.UTF_8)
  val MD5Sum: String = digest(FileBodyBytes)
}
