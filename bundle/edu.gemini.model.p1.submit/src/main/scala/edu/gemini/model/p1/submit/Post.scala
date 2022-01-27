package edu.gemini.model.p1.submit

import java.nio.charset.Charset
import java.net.HttpURLConnection
import edu.gemini.model.p1.immutable.{ProposalIo, Proposal}
import edu.gemini.model.p1.submit.SubmitResult._
import java.io._

private[submit] object Post {
  private val BOUNDARY = "134e7969a1b"

  def post(con: HttpURLConnection, p: Proposal): Either[Failure, Unit] = {
    con.setDoOutput(true)
    con.setRequestProperty("Content-Type", s"""multipart/form-data; boundary="$BOUNDARY"""")

    for {
      pdf1 <- p.meta.firstAttachment.toRight(ClientError("The proposal is missing PDF attachment 1.")).right
      pdf2 <- Right(p.meta.secondAttachment).right
      _    <- doPost(con, p, pdf1, pdf2).right
     } yield Unit
  }

  private def doPost(con: HttpURLConnection, p: Proposal, pdf1: File, pdf2: Option[File]): Either[Failure, Unit] = {
    val before = beforePdf(p)
    val after  = afterPdf()
    val between  = betweenPdf()
    val total  = before.size + pdf1.length + (if (pdf2.isDefined) between.size else 0L) + pdf2.map(_.length).getOrElse(0L) + after.size
    if (total > Int.MaxValue)
      Left(ClientError("PDF attachments too large"))
    else {
      con.setFixedLengthStreamingMode(total.toInt)
      for {
        os <- open(con).right
        _  <- before.post(os).right
        _  <- writeFile(pdf1, os).right
        _  <- Right(pdf2.map(_ => betweenPdf.post(os))).right
        _  <- pdf2.map(p => writeFile(p, os)).getOrElse(Right(())).right
        _  <- after.post(os).right
        _  <- op(os, _.close()).right
      } yield Unit
    }
  }

  private def open(con: HttpURLConnection): Either[Failure, OutputStream] =
    try {
      Right(con.getOutputStream)
    } catch {
      case e: Exception => Left(Offline(None))
    }

  private def beforePdf(p: Proposal): PostWriter = {
    val pw = new PostWriter(BOUNDARY)

    // Write the proposal
    pw.writeBoundary()
    pw.writeDisposition("proposal", "proposal.xml")
    pw.writeContentTypeXml()
    pw.blankLine()
    pw.write(ProposalIo.writeToString(p))
    pw.blankLine()

    // Write the PDF preamble
    pw.writeBoundary()
    pw.writeDisposition("attachment1", "attachment1.pdf")
    pw.writeContentTypePdf()
    pw.blankLine()
    pw
  }

  private def betweenPdf(): PostWriter = {
    val pw = new PostWriter(BOUNDARY)

    // Write the PDF preamble
    pw.writeBoundary()
    pw.writeDisposition("attachment2", "attachment2.pdf")
    pw.writeContentTypePdf()
    pw.blankLine()
    pw
  }

  private def afterPdf(): PostWriter = {
    val pw = new PostWriter(BOUNDARY)
    pw.writeClosingLine()
    pw
  }

  // grim imperative code separating out failures in reading the file vs
  // failures in writing to the url connection output stream
  private def writeFile(f: File, os: OutputStream): Either[Failure, Unit] = {
    val buf = new Array[Byte](1024 * 8)
    var bis: BufferedInputStream = null
    try {
      bis = new BufferedInputStream(new FileInputStream(f))
      var c = bis.read(buf)
      while (c != -1) {
        val error = op(os, _.write(buf, 0, c))
        if (error.isLeft) return error
        c = bis.read(buf)
      }
    } catch {
      case _: FileNotFoundException =>
        return Left(ClientError("Sorry, couldn't open the PDF attachment '%s' for reading.".format(f.getName)))
      case e: IOException           =>
        return Left(ClientError("Sorry, there was a problem reading '%s'%s.".format(
        f.getName, Option(e.getMessage).map(m => ": " + m).getOrElse(""))))
    } finally {
      if (bis != null) try { bis.close() } catch { case e: Exception => }
    }
    Right(Unit)
  }

  private def op(os: OutputStream, f: OutputStream => Unit): Either[Failure, Unit] =
    try {
      Right(f(os))
    } catch {
      case e: Exception => Left(Offline(None))
    }
}

private[submit] object PostWriter {
  val CHARSET  = Charset.forName("UTF-8")
  val CRLF     = "\r\n"
}

import PostWriter._

private[submit] class PostWriter(boundary: String) {
  private val baos = new ByteArrayOutputStream()

  def write(s: String): Unit =        { baos.write(s.getBytes(CHARSET)) }
  def writeLine(line: String): Unit = { write("%s%s".format(line, CRLF)) }
  def blankLine(): Unit =             { writeLine("") }
  def writeBoundary(): Unit =         { writeLine("--%s".format(boundary)) }

  def writeDisposition(name: String, filename: String): Unit = {
    writeLine(s"""Content-Disposition: form-data; name="$name"; filename="$filename" """)
  }

  def writeContentTypeXml(): Unit = {
    writeLine("""Content-Type: text/xml; charset=%s""".format(CHARSET.displayName))
  }

  def writeContentTypePdf(): Unit = {
    writeLine("Content-Type: application/pdf")
    writeLine("Content-Transfer-Encoding: binary")
  }

  def writeClosingLine(): Unit = {
    writeLine("--%s--".format(boundary))
  }

  def post(os: OutputStream): Either[Failure, Unit] =
    try {
      val buf = new BufferedOutputStream(os)
      baos.writeTo(buf)
      buf.flush()
      Right(Unit)
    } catch {
      case e: IOException => Left(Offline(None))
    }

  def size: Int = baos.size

}
