package edu.gemini.dataman.osgi

import edu.gemini.dataman.app.Dataman
import edu.gemini.dataman.core.{PollPeriod, DmanConfig, GsaAuth, GsaHost}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.Site
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
          gsaCommandsReg = Some(registerCommands(ctx, config, odb))

          Dataman.start(config, odb) <| { _.swap.foreach { f =>
            Log.log(Level.SEVERE, s"Could not start Data Manager: ${f.explain}", f.exception.orNull)
          }} | sys.error("Could not start Data Manager")

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
  }
}

object Activator {
  val Log = Logger.getLogger(Activator.getClass.getName)

  val CommandScope      = "osgi.command.scope"
  val CommandFunction   = "osgi.command.Function"

  val SummitHost        = "edu.gemini.dataman.gsa.summit.host"
  val ArchiveHost       = "edu.gemini.dataman.gsa.archive.host"
  val GsaAuth           = "edu.gemini.dataman.gsa.auth"
  val TonightPeriod     = "edu.gemini.dataman.poll.tonight"
  val ThisWeekPeriod    = "edu.gemini.dataman.poll.thisWeek"
  val AllProgramsPeriod = "edu.gemini.dataman.poll.allPrograms"

  private def readConfig(ctx: BundleContext): ValidationNel[String, DmanConfig] = {
    def lookup(name: String): ValidationNel[String, String] =
      Option(ctx.getProperty(name)).toSuccess(s"Missing '$name' property in app configuration".wrapNel)

    def lookupPollPeriod[A](name: String)(f: Duration => A): ValidationNel[String, A] =
      lookup(name).flatMap { timeString =>
        \/.fromTryCatch(Duration.parse(timeString)).leftMap { _ =>
          s"Couldn't parse $name property value '$timeString' as an ISO-8601 time duration."
        }.ensure(s"$name time value must be greater than zero.") { d =>
          !(d.isNegative || d.isZero)
        }.leftMap(_.wrapNel).map(f).validation
      }

    val archive = lookup(ArchiveHost).map(GsaHost.Archive)
    val summit  = lookup(SummitHost).map(GsaHost.Summit)
    val auth    = lookup(GsaAuth).map(a => new GsaAuth(a))
    val site    = Option(SiteProperty.get(ctx)).toSuccess(s"Missing or unparseable '${SiteProperty.NAME}' property.".wrapNel)

    val tonight  = lookupPollPeriod(TonightPeriod)(PollPeriod.Tonight)
    val thisWeek = lookupPollPeriod(ThisWeekPeriod)(PollPeriod.ThisWeek)
    val allProgs = lookupPollPeriod(AllProgramsPeriod)(PollPeriod.AllPrograms)

    val config = (archive |@| summit |@| auth |@| site |@| tonight |@| thisWeek |@| allProgs) { DmanConfig.apply }

    // DMAN TODO: we don't have a GS GSA test server or GS data so fix to GN for now
    config.map(_.copy(site = Site.GN))
  }

  private def registerCommands(ctx: BundleContext, config: DmanConfig, odb: IDBDatabaseService): ServiceRegistration[GsaCommands] = {
    val dict = new java.util.Hashtable[String, Object]() <|
      (_.put(CommandScope, "gsa"))                       <|
      (_.put(CommandFunction, Array("gsa")))

    ctx.registerService(classOf[GsaCommands], GsaCommands(config, odb), dict)
  }
}
