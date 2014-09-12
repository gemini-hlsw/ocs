package edu.gemini.spdb.shell.migrate

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}

import java.util.logging.{Level, Logger}
import scala.collection.JavaConverters._
import scala.util.Try
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal


/**
 * Migrate pre2014B observations.  Adds observing log and sequence
 * components.
 */
object Migrate2014B extends DBAbstractQueryFunctor {
  private val Log = Logger.getLogger(Migrate2014B.getClass.getName)

  def execute(db: IDBDatabaseService, n: ISPNode, ps: java.util.Set[Principal]): Unit = {
    val fact = db.getFactory
    val obs  = n.asInstanceOf[ISPObservation]
    val prog = obs.getProgram
    val key  = prog.getNodeKey

    lazy val name = Option(obs.getObservationID).fold(s"prog $key, obs ${obs.getNodeKey}") { _.stringValue() }

    SPNodeKeyLocks.instance.writeLock(key)

    Try {
      if (obs.getObsExecLog == null) obs.setObsExecLog(fact.createObsExecLog(prog, null))
      if (obs.getObsQaLog == null) obs.setObsQaLog(fact.createObsQaLog(prog, null))
    } recover {
      case t: Throwable => Log.log(Level.WARNING, s"Could not create an observing log for $name", t)
    }

    Try {
      if (obs.getSeqComponent == null) obs.setSeqComponent(fact.createSeqComponent(prog, SPComponentType.ITERATOR_BASE, null))
    } recover {
      case t: Throwable => Log.log(Level.WARNING, s"Could not create a sequence root for $name", t)
    }

    SPNodeKeyLocks.instance.writeUnlock(key)
  }

  def migrateAll(db: IDBDatabaseService): Unit = {
    val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)
    db.getQueryRunner(user).queryObservations(this)
  }

  def migrateOne(db: IDBDatabaseService, prog: ISPProgram, ps: java.util.Set[Principal]): Unit = {
    def migrateObs(o: ISPObservation): Unit = { execute(db, o, ps) }
    prog.getAllObservations.asScala.foreach(migrateObs)
    Option(prog.getTemplateFolder).foreach { tf =>
      tf.getTemplateGroups.asScala.foreach { tg =>
        tg.getAllObservations.asScala.foreach(migrateObs)
      }
    }
  }
}
