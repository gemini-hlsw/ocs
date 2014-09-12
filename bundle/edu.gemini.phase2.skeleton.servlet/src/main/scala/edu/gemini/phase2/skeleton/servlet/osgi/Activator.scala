package edu.gemini.phase2.skeleton.servlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.phase2.template.factory.api.TemplateFactory
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.Hashtable
import java.util.logging.Logger
import java.io.File
import edu.gemini.auxfile.copier.AuxFileCopier
import edu.gemini.phase2.skeleton.servlet.{SkeletonResetServlet, SkeletonServlet}
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

object Activator {
  val LOG = Logger.getLogger(getClass.getName)
  val APP_CONTEXT = "/skeleton"
  val RESET_APP_CONTEXT = "/reset"
  val AUXFILE_DIR_PROP = "edu.gemini.auxfile.root"
//  val NOTE_FILE_PROP   = "edu.gemini.skeleton.welcomeNote"
}

import Activator._

class Activator extends BundleActivator {

  private var serv: ServiceTracker[_,_] = null

  // We run as superuser
  val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  def start(ctx: BundleContext) {
    LOG.info("Start Skeleton Servlet")
    val text = Some(noteText)
    val root = auxfileRoot(ctx)
    serv = track[IDBDatabaseService, TemplateFactory, AuxFileCopier, HttpService, HttpService](ctx) { (odb, tfactory, copy, http) =>
      LOG.info(s"Registering $APP_CONTEXT and $RESET_APP_CONTEXT servlets")
      http.registerServlet(APP_CONTEXT,       new SkeletonServlet(odb, tfactory, root, text, copy, user), new Hashtable(), null)
      http.registerServlet(RESET_APP_CONTEXT, new SkeletonResetServlet(odb, tfactory, text, user),        new Hashtable(), null)
      http
    } { http =>
      http.unregister(APP_CONTEXT)
      http.unregister(RESET_APP_CONTEXT)
    }
    serv.open()
  }

  private def auxfileRoot(ctx: BundleContext): File = {
    val auxfileProp = Option(ctx.getProperty(AUXFILE_DIR_PROP))
    require(auxfileProp.isDefined, "%s property must be specified".format(AUXFILE_DIR_PROP))
    val auxfileRoot = new File(auxfileProp.get)
    require(auxfileRoot.exists() && auxfileRoot.canWrite && auxfileRoot.isDirectory, "%s is not a writable directory".format(auxfileRoot.getPath))
    auxfileRoot
  }

  val noteText =
    """
      |This initial Phase II program contains information including PI and support information, target lists, conditions constraints, and template observations based on the Phase I observing modes and resources. Following the technical assessment and ITAC queue-merging processes, some of your Phase I targets may have been deactivated, the total time adjusted, and/or observing condition constraints relaxed in order for your program to be scheduled in the queue band consistent with your ranking. The configuration information from the proposal has been added to template observations. Templates groups for science observations are found in the new Templates folder.  There is one template group for each requested configuration. Phase I targets with their associated conditions and total times are viewed by clicking on the template group node. Baseline calibration observations may be included in a separate "Baseline" folder.
      |
      |The Phase I proposal itself is no longer incorporated into the Phase II program, but the proposal document, PDF attachment, and PDF summary can be downloaded from the File Attachment tab on the top level program overview.
      |
      |PIs of Target of Opportunity (ToO) programs should consult with their NGO and Gemini contact scientists.
      |
      |The recommended process for non-ToO programs is as follows:
      |
      |1. FETCH THE EXAMPLE OT LIBRARIES since they contain additional examples and useful information such as lists of standard stars. Libraries should first be updated by selecting "Fetch Libraries" from the File menu. Thereafter they can be viewed by clicking on the Libraries button in the toolbar.
      |
      |2. UPDATE THE TEMPLATE OBSERVATIONS with the settings and sequence that will be applied to all targets in the group.  This can include setting central wavelengths and individual exposure times and changing the acquisition strategy. If different configurations are needed for different groups of targets, a template group can be split and a subset of the targets can be assigned to each group.
      |
      |3. APPLY THE TEMPLATES TO TARGETS AND CONDITIONS. It is possible to apply the templates to a subset of targets. New draft observations will be generated with target and conditions components added.
      |
      |4. MAKE TARGET-SPECIFIC CHANGES to the new draft observations. These changes may include setting the position angle, selecting guide stars, selecting telluric standards, etc. Don't forget to check that your guide stars are of sufficient brightness and are accessible at your specified orientations. Guide stars can be selected automatically using the "Auto GS" buttons.
      |
      |5. SET THE OBSERVATION STATUS TO "For Review" AND SYNC your observations by clicking the Sync button in the toolbar or by selecting "Sync changes..." from the File menu. Your NGO contact will check the observations and make any recommendations.
      |
      |We recommend that you complete all the observations for a single target and have them checked by your NGO contact before working on the remaining targets. We also recommend that where possible you make changes by updating the templates and then reapplying them to the targets. This will reduce the burden of making the same change to multiple observations.
      |
      |PIs who intend to copy observations from programs in previous semesters should also consult the current OT libraries to ensure that the configurations are up to date. You update the libraries by selecting "Fetch Libraries..." from the OT File menu. We also recommend that you replace any "Manual Arc" and "Manual Flat" nodes with the automatic Flat and Arc observation nodes to ensure that the observations will use the correct settings.
      |
      |Detailed information about the OT, instruments, the Phase II process, specific dates and instructions for the current and upcoming semesters, and tutorials can be found on the Gemini web site (goto www.gemini.edu/sciops). When online you can access the OT help page by selecting "OT Help" from the Help menu above. Review the Phase II keeping in mind any special instructions on the web pages.
      |
      |If you have any additional questions please submit a HelpDesk query at
      |
      |  http://www.gemini.edu/sciops/helpdesk/
    """.stripMargin

//  private def noteText(ctx: BundleContext): Option[String] =
//    Option(ctx.getProperty(NOTE_FILE_PROP)) map { fileName =>
//      val f = new File(fileName)
//      require(f.exists() && f.canRead, "Note text file %s doesn't exist or cannot be read".format(fileName))
//      val src = Source.fromFile(f, "UTF-8")
//      try {
//        src.mkString
//      } finally {
//        src.close()
//      }
//    }

  def stop(ctx: BundleContext) {
    LOG.info("Stop Skeleton Servlet")
    serv.close()
    serv = null
  }
}
