package edu.gemini.osgi.tools.app

//import OcsCredentials._

import com.jcraft.jsch.{JSch, Session, ChannelExec, ChannelSftp, SftpException}

import java.io.{ File, PrintWriter, InputStream, FileInputStream, FilterInputStream }
import java.nio.file.{Paths, Files, StandardCopyOption}
import java.nio.charset.StandardCharsets
import scala.io.Source

import java.util.jar.Manifest
import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder

class RPMDistHandler(jre: Option[String]) extends DistHandler {

  def build(wd: File, jreDir: Option[File], meta: ApplicationMeta, version:String, config: Configuration, d: Configuration.Distribution, solution: Map[BundleSpec, (File, Manifest)], log: sbt.Logger, appProjectBaseDir: File) {
    // Work in a subdir if we're compressing
    val execName = meta.executableName(version)
    val name = "%s_%s".format(execName, d.toString.toLowerCase)
    val outDir = mkdir(wd, name)

    // Common part
    buildCommon(outDir, meta, version, config, d, solution, appProjectBaseDir)

    // Shell script
    val scriptName = execName
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

    // The RPM spec file.
    val specPath = new File(wd, "rpm.spec")

    // Set up an SSH connection to the Linux VM where rpmbuild will be run.
    if (config.remoteBuildInfo.isEmpty)
      log.error("no remote build info: update OcsCredentials?")
    val remoteBuildInfo = config.remoteBuildInfo.get

    log.info(s"establishing ssh session to ${remoteBuildInfo.hostname}")
    JSch.setConfig("StrictHostKeyChecking", "no")
    val session = new JSch().getSession(remoteBuildInfo.username, remoteBuildInfo.hostname, remoteBuildInfo.port)
    session.setPassword(remoteBuildInfo.password)
    session.setTimeout(remoteBuildInfo.timeout)
    session.connect

    // Create an SFTP session to transfer to / from Linux VM.
    val rpmBuildDir = "rpmbuild"
    log.info("opening sftp channel")
    val sftp = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    sftp.connect(remoteBuildInfo.timeout)

    // ChannelSftp.mkdir fails with an SftpException if a directory already exists: handle this by allowing this exception.
    def rmkdir(dir: String): Unit =
      try {
	sftp.mkdir(dir)
      } catch {
	case e: SftpException =>
      }

    log.info("creating remote directories")
    rmkdir(rpmBuildDir)
    sftp.cd(rpmBuildDir)
    rmkdir("SOURCES")

    log.info("copying tarball")
    sftp.put(archiveName.getAbsolutePath, s"SOURCES/${name}.tar.gz")

    log.info("copying rpm spec file")
    sftp.put(specPath.getAbsolutePath, "rpm.spec")

    // Remote execution of rpmbuild.
    // Note that this will not work for '-test' releases, as rpmbuild does not accept '-' characters in versions.
    log.info("opening ssh channel")
    val ssh = session.openChannel("exec").asInstanceOf[ChannelExec]
    val cmd = s"cd $rpmBuildDir && rpmbuild -bb rpm.spec"
    ssh.setCommand(cmd)

    log.info("executing rpmbuild")
    ssh.connect(remoteBuildInfo.timeout)

    // Retrieve the constructed RPMs.
    log.info("retrieving rpm")
    val rpmOutputDir = mkdir(wd, "RPMS")
    sftp.cd("RPMS")
    val appName = execName.substring(0, execName.indexOf("_"))
    sftp.ls(s"${appName}*.rpm").foreach { f =>
      val fStr           = f.toString
      val sourceFilename = fStr.substring(fStr.lastIndexOf(" ")+1)
      val destFilename   = new File(rpmOutputDir, sourceFilename).toPath.toString
      sftp.get(sourceFilename, destFilename)
    }

    // Move the rpm back to wd
    val rpms = Files.newDirectoryStream(new File(wd, "RPMS").toPath, "*.rpm")
    rpms.iterator.foreach { f =>
       Files.move(f, wd.getParentFile.toPath.resolve(f.getFileName), StandardCopyOption.REPLACE_EXISTING)
    }

    // Remove our staging dir
    rm(wd)

  }

}

