package edu.gemini.pit.osgi

import edu.gemini.ui.workspace.IShellAdvisor
import edu.gemini.pit.ui.ShellAdvisor
import edu.gemini.pit.model.{AppMode, Model}
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.ags.client.api.AgsClient
import org.osgi.framework.{BundleActivator, BundleContext, ServiceReference}
import edu.gemini.pit.ui.robot.AgsRobot
import java.io.File
import java.util.Locale
import java.util.logging.Logger

import scalaz.\/

class Activator extends BundleActivator {
  private val Log = Logger.getLogger(Activator.this.getClass.getName)

  // We will track the AGS service, which can come and go
  var agsTracker:Option[ServiceTracker[AgsClient, AgsClient]] = None

  def start(context:BundleContext) {

    // UX-1217: Cannot enter numbers when commas are used as decimal point
    // Since we haven't really handled i18n ...
    val locale = Locale.getDefault
    Locale.setDefault(Locale.ENGLISH)

    // Turn this bundle property into the system property.
    // TODO: Not sure if this is used anymore.
    \/.fromTryCatchNonFatal(context.getProperty(AppMode.TestProperty).toBoolean).fold(
      _ => Log.warning(s"Context property ${AppMode.TestProperty} should be defined and have a boolean value."),
      v => System.setProperty(AppMode.TestProperty, v.toString)
    )

    // The way Workspace works is that you create an IShellAdvisor and register it as a service. Workspace sees this and
    // pops up a corresponding top-level window (an IShell). Because our shell advisor needs the ability to open new
    // shells and we don't want to leak OSGi abstractions, we pass this function to the ctor.
    def newShell(model:Model, file:Option[File]) {
      val adv = new ShellAdvisor(System.getProperty("edu.gemini.model.p1.schemaVersion"), model, file, newShell /* ! */, locale)
      context.registerService(classOf[IShellAdvisor].getName, adv, null)
    }

    // Create our first shell with an empty model and no associated file.
    newShell(Model.empty, None)

    // Create and open our AGS tracker
    agsTracker = Some(new AgsTracker(context))
    agsTracker.foreach(_.open())
  }

  def stop(context:BundleContext) {
    // Close our tracker.
    agsTracker.foreach(_.close())
    agsTracker = None
  }

}

// Track AGS service changes and pass them to the AgsRobot$ singleton. This is a little ugly but it hides the OSGI
// abstractions, which is what we want.
class AgsTracker(context:BundleContext) extends ServiceTracker[AgsClient, AgsClient](context, classOf[AgsClient].getName, null) {

  override def addingService(ref:ServiceReference[AgsClient]) = {
    val ags = context.getService(ref)
    AgsRobot.ags = Some(ags)
    ags
  }

  override def remove(ref:ServiceReference[AgsClient]) {
    AgsRobot.ags = None
    context.ungetService(ref)
  }

}