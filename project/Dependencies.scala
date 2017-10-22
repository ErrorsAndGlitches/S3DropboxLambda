import sbt._

object Dependencies {
  lazy val awsLambdaCore: ModuleID = "com.amazonaws" % "aws-lambda-java-core" % "1.0.0"
  lazy val awsLambdaEvents: ModuleID = "com.amazonaws" % "aws-lambda-java-events" % "1.0.0"

  lazy val dropboxCore: ModuleID = "com.dropbox.core" % "dropbox-core-sdk" % "2.1.1"

  lazy val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val mockito: ModuleID = "org.mockito" % "mockito-core" % "2.11.0"
}
