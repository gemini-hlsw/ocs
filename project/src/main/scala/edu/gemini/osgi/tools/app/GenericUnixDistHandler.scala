package edu.gemini.osgi.tools.app

import java.io.{ File, PrintWriter }

import java.util.jar.Manifest

class GenericUnixDistHandler(compress: Boolean, jre: Option[String]) extends DistHandler {

  def build(wd: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], log: sbt.Logger, appProjectBaseDir: File) {

    // Work in a subdir if we're compressing
    val scriptName = meta.executableName(version)
    val name = "%s_%s".format(meta.executableName(version), d.toString.toLowerCase) // pit_0.0.4_linux
    val outDir = if (compress) mkdir(wd, name) else wd

    // Common part
    buildCommon(outDir, meta, version, config, d, solution, appProjectBaseDir)

    // Shell script
    config.script foreach { s => copy(s, outDir) }
    if (config.script.isEmpty) {
      val unixFile = new File(outDir, scriptName)
      write(unixFile) { os =>
        closing(new PrintWriter(os)) { pw =>
          pw.println("#!/bin/bash")
          pw.println("cd `dirname \"$0\"`")
          pw.print(if (jre.isDefined) "./jre/bin/java" else "java")
          pw.print(config.vmargsWithApp(scriptName).mkString(" ", " ", ""))
          pw.print(" -jar " + solution(config.framework.bundleSpec)._1.getName)
          pw.print(config.args.mkString(" ", " ", ""))
          pw.println(" $@")
        }
      }

      import sys.process._
      val ret = "chmod 755 %s".format(unixFile.getPath).!
      if (ret != 0) sys.error("chmod returned " + ret)
    }

    // Copy the JRE (if any)
    jre foreach { path =>
      jreDir match {
        case None => sys.error("No JRE dir was specified.")
        case Some(jreDir) =>
          val jrePath = new File(jreDir, path)
          if (!jrePath.isDirectory) sys.error("No JRE was found at " + jrePath)
          jrePath.listFiles.foreach(copy(_, mkdir(outDir, "jre")))
      }
    }

    // Compress the results (if we're supposed to)
    if (compress) {
      val archiveName = name + ".tar.gz"
      val args = Array("tar", "-zcf", archiveName, name)
      val proc = Runtime.getRuntime.exec(args, Array[String]("COPYFILE_DISABLE=TRUE"), wd)
      val ret = proc.waitFor
      if (ret != 0) sys.error("tar returned " + ret)

      // Remove our staging dir
      rm(outDir)

    }

  }

}

