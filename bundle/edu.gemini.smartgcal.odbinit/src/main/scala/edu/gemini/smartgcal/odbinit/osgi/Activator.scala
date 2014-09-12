package edu.gemini.smartgcal.odbinit.osgi

import edu.gemini.smartgcal.odbinit.SmartGcalOdbInitializer
import edu.gemini.spModel.core.{Version, OcsVersionUtil}
import edu.gemini.util.osgi.ExternalStorage.getExternalDataFile
import BundleProp.toPositiveInt

import org.osgi.framework.{BundleContext, BundleActivator}

import java.io.File
import java.util.logging.{Level, Logger}

object Activator {
  val Log = Logger.getLogger(getClass.getName)

  val SpdbDirProp  = "edu.gemini.spdb.dir"
  val HostProp     = "edu.gemini.smartgcal.host"
  val PortProp     = "edu.gemini.smartgcal.port"
  val HttpsPort    = "org.osgi.service.http.port.secure"
  val IntervalProp = "edu.gemini.smartgcal.updateInterval"

  def initializer(ctx: BundleContext): Either[String, SmartGcalOdbInitializer] = {
    val prop = BundleProp(ctx)
    for {
      root     <- prop.withDefault(SpdbDirProp)(getExternalDataFile(ctx, "spdb"))(new File(_)).right
      host     <- prop.requiredString(HostProp).right
      port     <- prop.optional(PortProp)(toPositiveInt).right.flatMap(_.fold(prop.required(HttpsPort)(toPositiveInt))(Right(_))).right
      interval <- prop.withDefault(IntervalProp)(7200)(toPositiveInt).right
    } yield new SmartGcalOdbInitializer(new File(OcsVersionUtil.getVersionDir(root, Version.current), "gcal"), host, port, interval)
  }
}

import Activator._

class Activator extends BundleActivator {

  private var gCal: Option[SmartGcalOdbInitializer] = None

  override def start(ctx: BundleContext) {
    Log.info("Start SpModel Smart GCal")

    val ini = initializer(ctx)

    val (level, msg) = ini.fold(
      (Level.WARNING, _),
      ini => (Level.INFO, s"Smart GCal storage is at ${ini.getDirectory.getAbsolutePath}")
    )
    Log.log(level, msg)

    gCal = ini.right.toOption
    gCal foreach { _.start() }
  }

  override def stop(ctx: BundleContext) {
    gCal foreach { _.stop() }
    gCal = None
  }

}
