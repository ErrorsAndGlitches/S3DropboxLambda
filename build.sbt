target in assembly := new File("target/")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

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
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "3.6.0",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-kms" % "1.11.906",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.906",
    libraryDependencies += "com.dropbox.core" % "dropbox-core-sdk" % "3.1.5",
    libraryDependencies += "org.json4s" % "json4s-jackson_2.12" % "3.7.0-M1",
    libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.9.2",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "org.springframework" % "spring-core" % "5.3.1",
    libraryDependencies += "org.springframework" % "spring-beans" % "5.3.1",
    libraryDependencies += "org.springframework" % "spring-context" % "5.3.1",
    libraryDependencies += "javax.inject" % "javax.inject" % "1",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.1",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    libraryDependencies += "org.scalatestplus" %% "mockito-3-4" % "3.2.3.0" % Test,
    libraryDependencies += "org.mockito" % "mockito-core" % "3.6.0" % Test
  )

fork := true

addCommandAlias("run-dossier",
  """
    |;set javaOptions += "-Dlogback.configurationFile=configuration/logback.xml"
    |;runMain com.s3dropbox.lambda.DossierMain""".stripMargin.replace("\n", " ")
)