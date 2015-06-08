package edu.gemini.sp.vcs.reg.impl

import edu.gemini.sp.vcs.reg.{VcsRegistrationEvent, VcsRegistrar}
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.SPProgramID

import java.io.{PrintWriter, File}
import java.util.concurrent.Executors
import java.util.logging.{Level, Logger}
import scala.concurrent._
import scala.util.control.Exception._
import scala.xml.{PrettyPrinter, XML, Node}

/**
 * Manages VCS program id -> host/port registrations persistently.  Rather
 * gross.
 */
object VcsRegistrarImpl {
  private val LOG = Logger.getLogger(classOf[VcsRegistrarImpl].getName)

  private def load(storage: File): Either[Throwable, Map[SPProgramID, Peer]] = {
    def parseReg(reg: Node): (Peer, List[SPProgramID]) = {
      val loc = new Peer((reg \ "host").text, (reg \ "port").text.toInt, Site.tryParse((reg \ "site").text))
      val ids = reg.child.filter(_.label == "id").map(n => SPProgramID.toProgramID(n.text)).toList
      LOG.info("Assigning %s to %s".format(loc, ids.mkString(",")))
      loc -> ids
    }

    LOG.info("Load %s".format(storage))

    val res = allCatch either {
      val registrations = XML.loadFile(storage) match {
        case <registrations>{regs @_ *}</registrations> =>
          for {
            reg @ <reg>{_*}</reg> <- regs
          } yield parseReg(reg)
      }
      (registrations flatMap { case (loc, ids) => ids.map(_ -> loc) }).toMap
    }

    res.left.foreach { t =>
      LOG.log(Level.WARNING, "Could not load %s".format(storage.getPath), t)
    }

    res
  }

  private val printer = new PrettyPrinter(80, 2)

  private def store(storage: File, bak: File, m: Map[SPProgramID, Peer]) {
    // Converts Map[SPProgramID, VcsLocation] => Map[VcsLocation, List[SPProgramID]]
    val rev = (Map.empty[Peer, List[SPProgramID]] /: m) {
      case (res, tup) =>
        val (id, loc) = tup
        val lst = res.getOrElse(loc, List.empty[SPProgramID])
        res.updated(loc, id :: lst)
    } mapValues {
      _.sorted
    }

    val xml =
      <registrations>{
        rev map {
          case (loc, lst) =>
            <reg>
              <host>{loc.host}</host>
              <port>{loc.port}</port>
              <site>{loc.site}</site>
              {
                lst map {id => <id>{id}</id>}
              }
            </reg>
        }
      }</registrations>

    // Write the registrations to a temp file
    val tmp = File.createTempFile("vcs-reg", "txt", storage.getParentFile)
    val out = new PrintWriter(tmp)
    try {
      out.print(printer.format(xml))
    } finally {
      out.close()
    }

    // Backup the current temp file if it exists and move the temp registration
    // file.
    if (storage.exists()) {
      if (bak.exists() && !bak.delete()) LOG.warning("Could not delete backup vcs-reg storage file: %s".format(bak.getPath))
      if (!storage.renameTo(bak)) LOG.warning("Could not move storage file %s to backup %s".format(storage.getPath, bak.getPath))
    }
    if (!tmp.renameTo(storage)) LOG.severe("Could not move new vcs-reg file %s to %s".format(tmp.getPath, storage.getPath))
  }
}

import VcsRegistrarImpl._

class VcsRegistrarImpl(val storage: File) extends VcsRegistrar {
  val bak = new File(storage.getParentFile, storage.getName + ".bak")

  private def loadBackup: Map[SPProgramID, Peer] =
    if (bak.exists()) load(bak).left.map(Map.empty).merge
    else Map.empty

  private var m: Map[SPProgramID, Peer] =
    if (storage.exists()) load(storage).left.map(_ => loadBackup).merge
    else loadBackup

  private implicit val ctx = JavaConversions.asExecutionContext(Executors.newSingleThreadScheduledExecutor())

  def allRegistrations = m

  def registration(id: SPProgramID) = m.get(id)

  def register(pid: SPProgramID, loc: Peer): Unit =
    synchronized {
      if (m.get(pid).forall(_ != loc)) {
        m = m.updated(pid, loc)
        store(storage, bak, m)
        publish(pid, Some(loc))
      }
    }

  def unregister(pid: SPProgramID): Unit =
    synchronized {
      m.get(pid).foreach { _ =>
        m = m - pid
        store(storage, bak, m)
        publish(pid, None)
      }
    }

  private def publish(pid: SPProgramID, loc: Option[Peer]): Unit =
    future { publish(VcsRegistrationEvent(pid, loc)) }
}
