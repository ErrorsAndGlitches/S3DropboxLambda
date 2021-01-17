package com.s3dropbox.lambda

import com.amazonaws.services.s3.AmazonS3
import org.springframework.context.annotation.{AnnotationConfigApplicationContext, Configuration}

/**
 * A DossierMain is the main.
 */
@Configuration
case class DossierMain(var s3: AmazonS3, var dbxFiles: DbxFiles, var curManifest: Manifest) {

  def update(dossierBucket: String, dossierKey: String): Unit = {
    val dossierArtifacts: DossierArtifact = DossierArtifact(new ZipFileIterator(
      s3.getObject(dossierBucket, dossierKey).getObjectContent
    ))
    try {
      dbxFiles.deletePdfs(dossierArtifacts.filenamesToRemove(curManifest))
      dbxFiles.uploadPdfs(dossierArtifacts.zipFileEntriesToUpdate(curManifest))
      dbxFiles.uploadManifest(dossierArtifacts.dossierZipFileContents.manifest)
    } finally {
      dossierArtifacts.close()
      dbxFiles.shutdown()
    }
  }
}

object DossierMain {
  val SpringBasePackages: String = "com.s3dropbox.lambda"
  val DbxCredBucketBeanName: String = "dbxCredBucket"
  val DbxCredKeyBeanName: String = "dbxCredKey"
  val KmsIdBeanName: String = "kmsId"
  val AwsRegionBeanName: String = "awsRegion"

  def main(args: Array[String]): Unit = {
    DossierCliOpts.optionParser.parse(args, DossierCliOpts()) match {
      case Some(opts) => runDossier(opts)
      case _ => throw new IllegalStateException("Unable to parse CLI opts successfully")
    }
  }

  def runDossier(opts: DossierCliOpts): Unit = {
    val appContext: AnnotationConfigApplicationContext = new AnnotationConfigApplicationContext()
    appContext.registerBean(DbxCredBucketBeanName, classOf[String], () => opts.dbxCredBucket)
    appContext.registerBean(DbxCredKeyBeanName, classOf[String], () => opts.dbxCredKey)
    appContext.registerBean(KmsIdBeanName, classOf[String], () => opts.kmsId)
    appContext.registerBean(AwsRegionBeanName, classOf[String], () => opts.awsRegion)
    appContext.scan(SpringBasePackages)
    appContext.refresh()

    appContext.getBean(classOf[DossierMain]).update(opts.dossierBucket, opts.dossierKey)
  }
}
