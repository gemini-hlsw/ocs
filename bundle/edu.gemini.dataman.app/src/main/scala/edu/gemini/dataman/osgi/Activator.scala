package edu.gemini.dataman.osgi

import edu.gemini.dataman.app.Dataman
import edu.gemini.dataman.core.{PollPeriod, DmanConfig, GsaAuth, GsaHost}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.catchingNonFatal
import edu.gemini.spModel.core.osgi.SiteProperty
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{ServiceRegistration, BundleContext, BundleActivator}

import edu.gemini.dataman.osgi.Activator._
import org.osgi.util.tracker.ServiceTracker

import java.time.Duration
import java.util.logging.{Level, Logger}

import scalaz._
import Scalaz._

/** OSGi bundle activator.  Reads configuration, wires together the application,
  * and manages its lifecycle.
  */
final class Activator extends BundleActivator {

  var dmanCommandsReg: Option[ServiceRegistration[DatamanCommands]] = None
  var gsaCommandsReg: Option[ServiceRegistration[GsaCommands]] = None
  var tracker: Option[ServiceTracker[IDBDatabaseService, Dataman]] = None

  def start(ctx: BundleContext): Unit = {
    Log.info("start dataman app")

    def stopDataman(dman: Dataman): Unit = {
      Log.info("Stopping Data Manager.")
      dman.stop().swap.foreach { f =>
        Log.log(Level.SEVERE, f.explain, f.exception.orNull)
      }
    }

    readConfig(ctx) match {
      case Success(config)   =>
        Log.info(config.show)

        tracker = Some(Tracker.track[IDBDatabaseService, Dataman](ctx) { odb =>
          dmanCommandsReg = Some(registerCommands(ctx, "dataman", classOf[DatamanCommands], DatamanCommands()))
          gsaCommandsReg  = Some(registerCommands(ctx, "gsa", classOf[GsaCommands], GsaCommands(config, odb)))

          val dman = Dataman.start(config, odb)

          dman.swap.foreach { f =>
            Log.log(Level.SEVERE, s"Could not start Data Manager: ${f.explain}", f.exception.orNull)
          }

          dman | sys.error("Could not start Data Manager")

        }(stopDataman))

        tracker.foreach(_.open())

      case Failure(problems) =>
        val msg = s"Data Manager configuration issues.  Not starting.\n${problems.list.mkString("\n\t", "\n\t", "\n")}"
        Log.severe(msg)
        sys.error(msg)
    }
  }

  def stop(ctx: BundleContext): Unit = {
    Log.info("stop dataman app")

    tracker.foreach(_.close())
    tracker = None

    gsaCommandsReg.foreach(_.unregister())
    gsaCommandsReg = None

    dmanCommandsReg.foreach(_.unregister())
    dmanCommandsReg = None
  }
}

object Activator {
  val Log = Logger.getLogger(Activator.getClass.getName)

  val CommandScope     = "osgi.command.scope"
  val CommandFunction  = "osgi.command.Function"

  val SummitHost       = "edu.gemini.dataman.gsa.summit.host"
  val ArchiveHost      = "edu.gemini.dataman.gsa.archive.host"
  val GsaAuth          = "edu.gemini.dataman.gsa.auth"
  val ObsRefreshPeriod = "edu.gemini.dataman.poll.obsRefresh"

  val ArchivePeriods   = "edu.gemini.dataman.poll.archive"
  val SummitPeriods    = "edu.gemini.dataman.poll.summit"

  private def readConfig(ctx: BundleContext): ValidationNel[String, DmanConfig] = {
    def lookup(name: String): ValidationNel[String, String] =
      Option(ctx.getProperty(name)).toSuccess(s"Missing '$name' property in app configuration".wrapNel)

    def lookupPollPeriod[A](name: String)(f: Duration => A): ValidationNel[String, A] =
      lookup(name).flatMap { timeString =>
        catchingNonFatal(Duration.parse(timeString)).leftMap { _ =>
          s"Couldn't parse $name property value '$timeString' as an ISO-8601 time duration."
        }.ensure(s"$name time value must be greater than zero.") { d =>
          !(d.isNegative || d.isZero)
        }.leftMap(_.wrapNel).map(f).validation
      }

    val archive    = lookup(ArchiveHost).map(GsaHost.Archive)
    val summit     = lookup(SummitHost).map(GsaHost.Summit)
    val auth       = lookup(GsaAuth).map(a => new GsaAuth(a))
    val site       = Option(SiteProperty.get(ctx)).toSuccess(s"Missing or unparseable '${SiteProperty.NAME}' property.".wrapNel)
    val obsRefresh = lookupPollPeriod(ObsRefreshPeriod)(PollPeriod.ObsRefresh)

    import PollPeriod.{Tonight, ThisWeek, AllPrograms}
    def pollGroup[G <: PollPeriod.Group](prefix: String, ctor: (Tonight, ThisWeek, AllPrograms) => G): ValidationNel[String, G] = {
      val tonight  = lookupPollPeriod(s"$prefix.tonight")(PollPeriod.Tonight)
      val thisWeek = lookupPollPeriod(s"$prefix.thisWeek")(PollPeriod.ThisWeek)
      val allProgs = lookupPollPeriod(s"$prefix.allPrograms")(PollPeriod.AllPrograms)
      (tonight |@| thisWeek |@| allProgs) { ctor }
    }

    val archivePoll = pollGroup(ArchivePeriods, PollPeriod.Archive.apply)
    val summitPoll  = pollGroup(SummitPeriods, PollPeriod.Summit.apply)

    (archive |@| summit |@| auth |@| site |@| obsRefresh |@| archivePoll |@| summitPoll) {
      DmanConfig.apply
    }
  }

  private def registerCommands[C](ctx: BundleContext, name: String, clazz: Class[C], c: C): ServiceRegistration[C] = {
    val dict = new java.util.Hashtable[String, Object]() <|
      (_.put(CommandScope, name))                       <|
      (_.put(CommandFunction, Array(name)))

    ctx.registerService(clazz, c, dict)
  }
}
