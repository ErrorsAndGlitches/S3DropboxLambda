

target in assembly := new File("target/")

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com/s3dropbox/lambda",
        scalaVersion := "2.12.4",
        version := "1.0"
      )
    ),
    name := "S3DropboxLambda",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "2.0.1",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-kms" % "1.11.224",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.224",
    libraryDependencies += "com.dropbox.core" % "dropbox-core-sdk" % "2.1.1",
    libraryDependencies += "org.json4s" % "json4s-jackson_2.12" % "3.6.0-M1",
    libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.7.2",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    libraryDependencies += "org.mockito" % "mockito-core" % "2.11.0" % Test
  )
