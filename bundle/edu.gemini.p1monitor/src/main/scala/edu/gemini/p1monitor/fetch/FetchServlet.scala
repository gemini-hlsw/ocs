package edu.gemini.p1monitor.fetch

import edu.gemini.p1monitor.config.MonitoredDirectory
import edu.gemini.p1monitor.config.P1MonitorConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io._

/**
 * A servlet that fetches a proposal file.
 */
class FetchServlet(val config: P1MonitorConfig) extends HttpServlet {
  require(config != null)

  protected override def doGet(sreq: HttpServletRequest, sres: HttpServletResponse): Unit = {
    val req = try {
      new FetchRequest(sreq)
    } catch {
      case ex: FetchException =>
        throw new ServletException(ex.getMessage)
    }
    try {
      sendFile(req, sres)
    }
    catch {
      case ex: FileNotFoundException => {
        sres.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find the proposal.")
      }
      case ex: FetchException => {
        throw new ServletException(ex.getMessage)
      }
    }
  }

  private def getRequestedFile(req: FetchRequest): File = {
    val ptc = config.getDirectory(req.dir)
    if (ptc == null) {
      throw new FetchException("Directory inexistant: " + req.dir)
    }
    val dir = ptc.dir
    val proposalPrefix = req.proposalType
    val fileName = "%s_%s".format(proposalPrefix, req.proposal)
    val res = req.format match {
      case FetchFormat.xml => new File(dir, fileName + ".xml")
      case FetchFormat.pdf => new File(dir, fileName + "_summary.pdf")
      case FetchFormat.attachment => new File(dir, fileName + ".pdf")
      case FetchFormat.attachment2 => new File(dir, fileName + "_stage2.pdf")
    }
    if (!(res.exists && res.canRead)) {
      throw new FileNotFoundException("Could not find '" + fileName + "'")
    }
    res
  }

  private def getExtension(f: File): String = {
    f.getName.indexOf(".") match {
      case i if i < 0  => ""
      case i           => f.getName.substring(i)
    }
  }

  private def sendFile(req: FetchRequest, res: HttpServletResponse): Unit = {
    val f = getRequestedFile(req)
    val format = req.format
    val proposalPrefix:String = req.proposalType
    val filename= req.format match {
      case FetchFormat.pdf => "%s_%s_summary%s".format(proposalPrefix, req.proposal, getExtension(f))
      case _ => "%s_%s%s".format(proposalPrefix, req.proposal, getExtension(f))
    }

    // TODO Remove side effects
    val disposition = format match {
      case FetchFormat.pdf =>
        res.setContentType("application/pdf")
        "inline; filename=" + filename
      case FetchFormat.xml =>
        res.setContentType("application/xml")
        "attachment; filename=" + filename
      case FetchFormat.attachment =>
        res.setContentType("application/pdf")
        "inline; filename=" + filename
      case FetchFormat.attachment2 =>
        res.setContentType("application/pdf")
        "inline; filename=" + filename
    }
    res.setHeader("Content-Disposition", disposition)
    // TODO make it scalish
    var bin: BufferedInputStream = null
    var bos: BufferedOutputStream = null
    try {
      bin = new BufferedInputStream(new FileInputStream(f))
      bos = new BufferedOutputStream(res.getOutputStream)
      val buf: Array[Byte] = new Array[Byte](8 * 1024)
      var len: Int = 0
      while ( {
        len = bin.read(buf)
        len
      } > 0) {
        bos.write(buf, 0, len)
      }
      bos.flush
    }
    finally {
      try {
        if (bin != null) {
          bin.close
        }
      }
      catch {
        case ex: IOException => {
        }
      }
      try {
        if (bos != null) {
          bos.close
        }
      }
      catch {
        case ex: IOException => {
        }
      }
    }
  }

}
