package edu.gemini.osgi.tools.app

import java.io.{ File, PrintWriter, InputStream, FileInputStream, FilterInputStream }
import java.nio.file.{Paths, Files, StandardCopyOption}
import java.nio.charset.StandardCharsets
import scala.io.Source

import java.util.jar.Manifest
import scala.collection.JavaConversions._

class RPMDistHandler(jre: Option[String]) extends DistHandler {

  def build(wd: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], appProjectBaseDir: File) {
    // Work in a subdir if we're compressing
    val scriptName = meta.executableName(version)
    val name = "%s_%s".format(meta.executableName(version), d.toString.toLowerCase)
    val outDir = mkdir(wd, name)

    // Due to permission issues and limitations on rpmbuild we need to override .rpmmacros
    val rpmbuild = wd // Hardcoded to avoid permission issues
    // Set topdir and prevent the binary stripping step on RPM creation
    val rpmmacros = s"%_topdir $rpmbuild\n%__os_install_post %{nil}\n"

    // Overwrite it, it changes per project
    val rpmmacrosFiles = new File(System.getProperty("user.home"), ".rpmmacros")
    Files.write(rpmmacrosFiles.toPath, rpmmacros.getBytes(StandardCharsets.UTF_8))

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

    // Copy spec and replace version on the SPEC file
    val specFile = config.spec
    specFile.foreach { s =>
      // JavaApplicationStub into macos
      val jas = new File(wd, "rpm.spec")
      Files.copy(s.toPath, jas.toPath)
      // Replace
      val lines = Source.fromFile(jas).getLines // use fromFile here 
      val  out = for {
          in  <- lines
        } yield in.replaceAll("@VERSION@", version)
      Files.write(jas.toPath, out.mkString("\n").getBytes(StandardCharsets.UTF_8))
    }

    // Compress the results
    val sourcesDir = mkdir(wd, "SOURCES")
    val archiveName = new File(sourcesDir, name + ".tar.gz")
    val args = Array("tar", "-zcf", archiveName.getAbsolutePath, name)
    val proc = Runtime.getRuntime.exec(args, Array[String]("COPYFILE_DISABLE=TRUE"), wd)
    val ret = proc.waitFor
    if (ret != 0) sys.error("tar returned " + ret)

    // Now create the RPM
    val specPath = new File(wd, "rpm.spec")
    val rpmArgs = Array("rpmbuild", "-bb", s"${specPath.getAbsolutePath}")
    val result = Runtime.getRuntime.exec(rpmArgs).waitFor()
    if (result != 0) {
      println("*** " + rpmArgs.mkString(" "))
      println("*** rpmbuild returned " + result)
    }

    // Move the rpm back to wd
    val rpms = Files.newDirectoryStream(new File(rpmbuild, "RPMS").toPath, "*.rpm")
    rpms.iterator.foreach { f =>
       Files.move(f, wd.getParentFile.toPath.resolve(f.getFileName), StandardCopyOption.REPLACE_EXISTING)
    }

    // Remove our staging dir
    rm(wd)

  }

}

