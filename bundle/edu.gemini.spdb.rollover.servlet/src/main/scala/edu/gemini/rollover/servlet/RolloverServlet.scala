package edu.gemini.rollover.servlet

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.spModel.rich.pot.spdb._

import java.io.{OutputStreamWriter, BufferedOutputStream}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import javax.servlet.http.HttpServletResponse._
import scala.collection.JavaConverters._
import scala.xml.{XML, Elem}
import java.util.logging.{Level, Logger}
import java.security.Principal

object RolloverServlet {
  val LOG = Logger.getLogger(getClass.getName)
}

final class RolloverServlet(site: Site, odb: IDBDatabaseService, user: java.util.Set[Principal]) extends HttpServlet {

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) {
    doGet(req, res)
  }

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
    val sem       = semester(req)
    val validProg = IncludeProgram(site, sem)
    val validObs  = IncludeObservation

    def extractRolloverObs(progs: List[ISPProgram]): List[RolloverObservation] =
      for {
        p <- progs if validProg(p)
        o <- p.getAllObservations.asScala.toList if validObs(o)
        ro <- RolloverObservation.toRollover(o).toList
      } yield ro

    val roxml: Either[OdbError, Elem] =
      odb.query(user)(progs => toXml(sem, extractRolloverObs(progs)))

    // Do the right (or left) thing ...
    roxml.right foreach { xml => send(xml, res) }
    roxml.left foreach { err =>
      RolloverServlet.LOG.log(Level.WARNING, "problem getting rollover obs: " + err.msg, err.remoteException.orNull)
      res.sendError(SC_INTERNAL_SERVER_ERROR, err.msg)
    }
  }

  def toXml(sem: Semester, lst: List[RolloverObservation]): Elem =
    <rollover site={site.abbreviation} semester={sem.toString} timestamp={System.currentTimeMillis().toString}>
      {lst map { toXml }}
    </rollover>

  private def toXml(ro: RolloverObservation): Elem =
    <obs>
      <id>{ ro.id.toString }</id>
      <partner>{ ro.partner }</partner>
      <target>
        <ra>{ ro.target.coords.map(_.ra).getOrElse(0.0) }</ra>
        <dec>{ ro.target.coords.map(_.dec).getOrElse(0.0) }</dec>
      </target>
      <conditions>
        <cc>{ ro.conds.cc.getPercentage }</cc>
        <iq>{ ro.conds.iq.getPercentage }</iq>
        <sb>{ ro.conds.sb.getPercentage }</sb>
        <wv>{ ro.conds.wv.getPercentage }</wv>
      </conditions>
      <time>{ ro.remainingTime }</time>
    </obs>

  private def send(xml: Elem, res: HttpServletResponse) {
    res.setContentType("text/xml; charset=UTF-8")
    val w = new OutputStreamWriter(new BufferedOutputStream(res.getOutputStream), "UTF-8")
    try {
      XML.write(w, xml, "UTF-8", true, null)
    } catch {
      case ex: Exception => RolloverServlet.LOG.log(Level.WARNING, "problem sending response", ex)
    } finally {
      w.close()
    }
  }

  def semester(req: HttpServletRequest): Semester =
    Option(req.getParameter("semester")) map { Semester.parse } getOrElse new Semester(site)
}
