package edu.gemini.phase2.skeleton.auxfile

import java.io.{FileInputStream, FileOutputStream, PrintWriter, File}

private[auxfile] object FileOps {
  type FileUpdate = Either[FileError, Unit]
}

private[auxfile] case class FileOps(f: File) {
  import FileOps.FileUpdate

  private def error(ex: Exception): FileUpdate = Left(FileError(f, ex))
  private def error(msg: String): FileUpdate   = Left(FileError(f, msg.format(f.getPath)))

  private def trueOrError(p: => Boolean, msg: => String): FileUpdate =
    if (p) Right(()) else error(msg)

  private def doOrError(block: => Unit): FileUpdate =
    try {
      block
      Right(())
    } catch {
      case ex: Exception => error(ex)
    }


  def eWrite(s: String): FileUpdate =
    doOrError {
      val out = new PrintWriter(f)
      try { out.print(s) } finally { out.close() }
    }

  def eSetReadOnly: FileUpdate =
    trueOrError(f.setReadOnly(), "Couldn't set file %s to read only")

  def eSetWritable: FileUpdate =
    trueOrError(f.canWrite || f.setWritable(true), "Could not make %s writable")

  def eCreateWritableDir: FileUpdate =
    createWritable(_.isDirectory, _.mkdirs(), "directory")

  def eCreateWritableFile: FileUpdate =
    createWritable(_.isFile, _.createNewFile(), "file")

  private def createWritable(test: File=>Boolean, make: File=>Boolean, kind: String): FileUpdate =
    if (f.exists())
      if (test(f)) eSetWritable else error("%s exists but is not a " + kind)
    else
      if (make(f)) eSetWritable else error("Could not make " + kind + " %s")

  def eCopyTo(dest: File): FileUpdate =
    for {
      _ <- dest.eCreateWritableFile.right
      _ <- copyToExisting(dest).right
    } yield ()

  private def copyToExisting(dest: File): FileUpdate =
    doOrError {
      new FileOutputStream(dest).getChannel.transferFrom(new FileInputStream(f).getChannel, 0, Long.MaxValue)
    }

  def eMoveTo(dest: File): FileUpdate =
    trueOrError(f.renameTo(dest), "Couldn't move %s to " + dest.getPath)
}
