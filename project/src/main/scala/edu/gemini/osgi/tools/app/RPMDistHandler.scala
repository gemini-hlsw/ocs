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
        case None         => sys.error("No JRE dir was specified.")
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
    val remoteBuildInfo = config.remoteBuildInfo.getOrElse {
      sys.error("No remote build info: update OcsCredentials?")
    }

    log.info(s"Establishing ssh session to ${remoteBuildInfo.hostname}.")
    JSch.setConfig("StrictHostKeyChecking", "no")
    val session = new JSch().getSession(remoteBuildInfo.username, remoteBuildInfo.hostname, remoteBuildInfo.port)
    session.setPassword(remoteBuildInfo.password)
    session.setTimeout(remoteBuildInfo.timeout)
    session.connect

    // Create an SFTP session to transfer to / from Linux VM.
    val rpmBuildDir = "rpmbuild"
    log.info("Opening sftp channel.")
    val sftp = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    sftp.connect(remoteBuildInfo.timeout)

    // ChannelSftp.mkdir fails with an SftpException if a directory already exists: handle this by allowing this exception.
    def remoteMkdir(dir: String): Unit =
      try {
	sftp.mkdir(dir)
      } catch {
	case e: SftpException =>
      }

    // Create remote directories if they don't exist and cd into rpmbuild since we do everything in here.
    log.info("Creating remote directories.")
    remoteMkdir(rpmBuildDir)
    sftp.cd(rpmBuildDir)
    remoteMkdir("SOURCES")

    log.info("Copying tarball.")
    val destArchiveFile = s"SOURCES/${name}.tar.gz"
    sftp.put(archiveName.getAbsolutePath, destArchiveFile)

    log.info("Copying RPM spec file.")
    val destSpecFile = "rpm.spec"
    sftp.put(specPath.getAbsolutePath, destSpecFile)

    // Remote execution of rpmbuild.
    // Note that this will not work for '-test' releases, as rpmbuild does not accept '-' characters in versions.
    log.info("Opening ssh channel.")
    val ssh = session.openChannel("exec").asInstanceOf[ChannelExec]
    val cmd = s"rm -rf /home/software/.rpmmacros && cd $rpmBuildDir && rpmbuild -bb $destSpecFile"
    ssh.setCommand(cmd)

    log.info("Executing remote rpmbuild.")
    ssh.connect(remoteBuildInfo.timeout)

    // Unfortunately, the only way to wait for rpmbuild to finish is to continuously poll until isClosed returns true.
    while (!ssh.isClosed) Thread.sleep(1000)
    ssh.disconnect

    // Retrieve the constructed RPMs.
    log.info("Retrieving RPM.")
    val rpmOutputDir = mkdir(wd, "RPMS")

    val rpmName = execName.replace("_", "-")
    sftp.ls(s"RPMS/${rpmName}*.rpm").foreach { f =>
      val fStr           = f.toString
      val remoteRpmFilename = fStr.substring(fStr.lastIndexOf(" ")+1)
      val remoteRpmFile     = s"RPMS/$remoteRpmFilename"
      val localRpmFile      = new File(rpmOutputDir, remoteRpmFilename).toPath.toString
      sftp.get(remoteRpmFile, localRpmFile)
      sftp.rm(remoteRpmFile)
    }
    sftp.disconnect

    // Cleanup. We need another ssh session to do this properly on the build machine, because ChannelSftp has an rmdir, but
    // if that directory contains any other files, an SftpException is thrown with a SSH_FX_FAILURE (general failure) and
    // there is no rm -r.
    log.info("Opening ssh channel for cleanup.")
    val cleanupSsh = session.openChannel("exec").asInstanceOf[ChannelExec]
    val cleanupCmd = s"""rm -rf \"${rpmBuildDir}\""""
    cleanupSsh.setCommand(cleanupCmd)
    log.info("Executing remote cleanup.")
    cleanupSsh.connect(remoteBuildInfo.timeout)
    while (!cleanupSsh.isClosed) Thread.sleep(1000)
    cleanupSsh.disconnect

    // Furthermore, JSch fails with an error SSH_FX_FAILURE (general failure) if you try to rm a directory that contains
    // other files, and there seems to be n
    // sftp.rm(destArchiveFile)
    // sftp.rm(destSpecFile)
    session.disconnect

    // Move the rpm back to wd
    val rpms = Files.newDirectoryStream(new File(wd, "RPMS").toPath, "*.rpm")
    rpms.iterator.foreach { f =>
       Files.move(f, wd.getParentFile.toPath.resolve(f.getFileName), StandardCopyOption.REPLACE_EXISTING)
    }

    // Remove our staging dir
    rm(wd)

  }

}

