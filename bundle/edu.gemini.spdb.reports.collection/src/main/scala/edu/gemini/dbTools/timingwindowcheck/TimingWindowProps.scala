package edu.gemini.dbTools.timingwindowcheck

import java.io.{FileOutputStream, FileInputStream, File}
import java.time.Instant
import java.util.Properties

import scalaz._
import Scalaz._
import scalaz.effect.IO

/**
 * Permanent state needed between invocations of the timing window check cron
 * job. In particular we keep up with the last time we executed in order to
 * only warn about timing windows that have closed since then.
 */
final case class TimingWindowProps(when: Instant) {
  import TimingWindowProps._

  def toProperties: Properties = {
    val p = new Properties()
    p.setProperty(Key, when.toString) // writes in a format readable by `Instant.parse`
    p
  }

  def store(dir: File): IO[Unit] =
    IO(new FileOutputStream(file(dir)))
      .bracket(fos => IO(fos.close())) { fos =>
        IO(toProperties.store(fos, "TimingWindowCheck cron properties"))
      }

}

object TimingWindowProps {
  val Key = "LastCheck"

  def fromProperties(p: Properties): Option[TimingWindowProps] =
    Option(p.getProperty(Key)).map(s => TimingWindowProps(Instant.parse(s)))

  def file(dir: File): File =
    new File(dir, "timingWindow.properties")

  def load(dir: File): IO[Option[TimingWindowProps]] = {

    def open(f: File): IO[Option[FileInputStream]] =
      IO((f.exists && f.canRead) option new FileInputStream(f))

    def load(fis: FileInputStream): IO[Option[TimingWindowProps]] =
      IO {
        val p = new Properties()
        p.load(fis)
        fromProperties(p)
      }

    open(file(dir))
      .bracket(fis => IO(fis.foreach(_.close()))) {
        _.fold(IO(Option.empty[TimingWindowProps]))(load)
      }
  }

}
