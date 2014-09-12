package edu.gemini.auxfile.workflow.osgi

import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.util.tracker.ServiceTracker
import java.util.logging.Logger
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.auxfile.copier.AuxFileCopier
import edu.gemini.auxfile.workflow.{Mailer, CopyTaskState, Workflow}
import edu.gemini.auxfile.api.AuxFileListener

object Activator {
  val LOG = Logger.getLogger(getClass.getName)
}

import Activator._

class Activator extends BundleActivator {
  private var serv: Option[ServiceTracker[_,_]] = None

  def start(ctx: BundleContext): Unit = {
    LOG.info("Start edu.gemini.auxfile.workflow")
    val mc  = new OsgiMailConfig(ctx)

    serv = Some(track[IDBDatabaseService, AuxFileCopier, (() => Unit)](ctx) { (odb, cp) =>
      val cts = new CopyTaskState(cp, ctx.getDataFile("copyTaskState.txt"))
      val wfl = new Workflow(cts, new Mailer(mc, odb))
      wfl.start()

      val reg = ctx.registerService(classOf[AuxFileListener], wfl, null)

      // Cleanup
      () => {
        reg.unregister()
        wfl.stop()
      }

    } { _.apply() })

    serv.foreach(_.open())
  }

  def stop(ctx: BundleContext) {
    LOG.info("Stop edu.gemini.auxfile.workflow")
    serv.foreach(_.close())
    serv = None
  }
}
