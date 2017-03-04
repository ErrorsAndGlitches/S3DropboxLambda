import Dependencies._

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com/s3dropbox/lambda",
        scalaVersion := "2.12.1",
        version := "1.0"
      )
    ),
    name := "S3DropboxLambda",
    libraryDependencies += awsLambdaCore,
    libraryDependencies += awsLambdaEvents,
    libraryDependencies += scalaTest % Test
  )
