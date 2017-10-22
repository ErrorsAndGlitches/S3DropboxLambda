package com.s3dropbox.lambda

import java.nio.file.attribute.FileTime

class DbxFileStatusSpec extends UnitSpec {

  describe("When Dropbox does not have the file") {
    it("Should indicate that an update is needed") {

      val dbxPdfs: DbxFileStatus = DbxFileStatus(DbxFileStatus.ROOT_FOLDER, new MockDbx(List(List(
        FileData("file_0", MockDbx.date(2014, 1, 1)),
        FileData("file_1", MockDbx.date(2014, 1, 2))
      ))).dbxUserFileRequests())

      assert(dbxPdfs.shouldUpdate("file_2", FileTime.from(MockDbx.instant(2014, 1, 3))))
    }
  }

  describe("When Dropbox has the file and the file is older on the server side") {
    it("Should indicate that an update is needed") {

      val dbxPdfs: DbxFileStatus = DbxFileStatus(DbxFileStatus.ROOT_FOLDER, new MockDbx(List(List(
        FileData("file_0", MockDbx.date(2014, 1, 1)),
        FileData("file_1", MockDbx.date(2014, 1, 2))
      ))).dbxUserFileRequests())

      assert(dbxPdfs.shouldUpdate("file_1", FileTime.from(MockDbx.instant(2014, 1, 3))))
    }
  }

  describe("When Dropbox has file and the file is newer on the server side") {
    it("Should indicate that an update is not needed") {

      val dbxPdfs: DbxFileStatus = DbxFileStatus(DbxFileStatus.ROOT_FOLDER, new MockDbx(List(List(
        FileData("file_0", MockDbx.date(2014, 1, 1)),
        FileData("file_1", MockDbx.date(2014, 1, 2))
      ))).dbxUserFileRequests())


      assert(!dbxPdfs.shouldUpdate("file_1", FileTime.from(MockDbx.instant(2013, 1, 1))))
    }
  }

  describe("When Dropbox has the file found in the second list fetch and the file is older on the server side") {
    it("Should indicate that an update is needed") {
    }
  }
}
