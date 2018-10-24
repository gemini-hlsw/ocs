package edu.gemini.dbTools.archive


import edu.gemini.spdb.cron.CronStorage
import org.osgi.framework.BundleContext
import edu.gemini.dbTools.html.{FtpProps, FtpUtil}
import edu.gemini.pot.client.SPDB
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.spModel.io.app.ExportXmlApp
import edu.gemini.util.security.auth.keychain.KeyServer

import java.io._
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.annotation.tailrec
import scalaz._
import Scalaz._
import scalaz.effect._
import java.security.Principal


object Archiver {

  val ARCHIVE_DIRECTORY_PROP = "edu.gemini.dbTools.archive.directory"

  def run(ctx: BundleContext)(store: CronStorage, log: Logger, env: java.util.Map[String, String], user: java.util.Set[Principal]): Unit = {
    val archiveDir = Option(ctx.getProperty(ARCHIVE_DIRECTORY_PROP)).fold(store.tempDir) { dirName =>
      new File(dirName)
    }

    val ksref = ctx.getServiceReference(classOf[KeyServer])
    val ks: KeyServer = ctx.getService(ksref)

    val vlref = ctx.getServiceReference(classOf[VcsLog])
    val vl: VcsLog = ctx.getService(vlref)

    try {
      archiveAndFtp(log, archiveDir, new FtpProps(env), ks, vl, user).unsafePerformIO
    } finally {
      ctx.ungetService(vlref)
      ctx.ungetService(ksref)
    }
  }

  def archiveAndFtp(log: Logger, tempDir: File, config: FtpProps, ks: KeyServer, vl: VcsLog, user: java.util.Set[Principal]): IO[Unit] =
    for {
      f <- new Archiver(log ,ks, vl, user).run(tempDir)
      _ <- IO(FtpUtil.sendFile(log, f, config))
    } yield ()

}

class Archiver(log: Logger, ks: KeyServer, vl: VcsLog, user: java.util.Set[Principal]) {

  def run(tempDir: File): IO[File] =
    for {
      d <- mkdir(tempDir)
      _ <- export(d)
      z <- zip(d)
      _ <- cleanup(d)
    } yield z

  def mkdir(tempDir: File): IO[File] =
    for {
      n <- IO(new Date) map (new SimpleDateFormat("yyyyMMdd-HHmm").format)
      d <- IO(new File(new File(tempDir, "archive"), n))
      _ <- IO(d.mkdirs)
    } yield d

  def export(dir: File): IO[Unit] =
    for {
      _ <- IO(log.info(s"exporting to $dir"))
      _ <- IO(new ExportXmlApp(SPDB.get, user).exportAll(dir))
      _ <- ks.backup(new File(dir, "keydb.zip")).run
      _ <- IO(vl.archive(new File(dir, "vcsdb.zip")))
    } yield ()

  def zip(dir: File): IO[File] =
    for {
      z <- IO(new File(dir.getParentFile, dir.getName + ".zip"))
      _ <- IO(log.info(s"writing zipfile $z"))
      _ <- IO(new FileOutputStream(z)).bracket(a => IO(a.close)) { a =>
        IO(new ZipOutputStream(a)).bracket(b => IO(b.close)) { b =>
          for {
            _ <- xmlFiles(dir) >>= (_.traverseU(add(b)))
            _ <- zipFiles(dir) >>= (_.traverseU(add(b)))
          } yield ()
        }
      }
    } yield z

  def cleanup(dir: File): IO[Unit] =
    for {
      _ <- IO(log.info("cleaning up..."))
      _ <- IO(dir.listFiles.toList) >>= (_.traverseU(f => IO(f.delete)))
      _ <- IO(dir.delete)
    } yield ()

  def xmlFiles(dir: File): IO[List[File]] =
    filterFiles(dir, ".xml")

  def zipFiles(dir: File): IO[List[File]] =
    filterFiles(dir, ".zip")

  def filterFiles(dir: File, ext: String): IO[List[File]] =
    IO(dir.listFiles.toList.filter(_.getName.toLowerCase.endsWith(ext)))

  def add(zos: ZipOutputStream)(f: File): IO[Unit] =
    for {
      e <- zipEntry(f)
      _ <- IO(zos.putNextEntry(e))
      _ <- copyFile(f, zos)
      _ <- IO(zos.closeEntry)
    } yield ()

  def zipEntry(f: File): IO[ZipEntry] =
    for {
      e <- IO(new ZipEntry(s"${f.getParentFile.getName}/${f.getName}"))
      _ <- IO(e.setSize(f.length()))
      _ <- IO(e.setTime(f.lastModified()))
    } yield e

  def copyFile(in: File, out: OutputStream): IO[Unit] =
    IO(new FileInputStream(in)).bracket(s => IO(s.close))(copy(out))

  def copy(out: OutputStream)(in: InputStream): IO[Unit] =
    IO {
      val buf = new Array[Byte](1024 * 64)
      @tailrec def go(): Unit = {
        val len = in.read(buf)
        if (len >= 0) {
          out.write(buf, 0, len)
          go()
        }
      }
      go()
    }
}
