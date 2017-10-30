
import sbt.{ State => _, Configuration => _, Show => _, _ }
import Keys._

trait OcsBundle {

  // Bundle projects.
  // Inter-project dependencies must be declared here.

  lazy val bundle_edu_gemini_ags_servlet =
    project.in(file("bundle/edu.gemini.ags.servlet")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_ags
    )

  lazy val bundle_edu_gemini_auxfile_workflow =
    project.in(file("bundle/edu.gemini.auxfile.workflow")).dependsOn(
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_fits,
      bundle_edu_gemini_util_javax_mail,
      bundle_edu_gemini_util_ssh,
      bundle_edu_gemini_util_trpc
    )

  lazy val bundle_edu_gemini_dataman_app =
    project.in(file("bundle/edu.gemini.dataman.app")).dependsOn(
      bundle_edu_gemini_util_file_filter,
      bundle_edu_gemini_pot % "test->test;compile->compile",
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_gsa_query % "test->test;compile->compile"
    )

  lazy val bundle_edu_gemini_gsa_query =
    project.in(file("bundle/edu.gemini.gsa.query")).dependsOn(
      bundle_edu_gemini_pot % "test->test;compile->compile",
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_edu_gemini_horizons_api =
    project.in(file("bundle/edu.gemini.horizons.api")).dependsOn(
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_coords
    )

  lazy val bundle_edu_gemini_itc_shared =
    project.in(file("bundle/edu.gemini.itc.shared")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_trpc,
      bundle_edu_gemini_auxfile_workflow
    )

  lazy val bundle_edu_gemini_itc =
    project.in(file("bundle/edu.gemini.itc")).dependsOn(
      bundle_edu_gemini_itc_shared,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_pot
    )

  lazy val bundle_edu_gemini_itc_web =
    project.in(file("bundle/edu.gemini.itc.web")).dependsOn(
      bundle_edu_gemini_itc % "test->test;compile->compile",
      bundle_edu_gemini_itc_shared,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_pot
    )

  lazy val bundle_edu_gemini_lchquery_servlet =
    project.in(file("bundle/edu.gemini.lchquery.servlet")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi,
      bundle_jsky_util,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_obslog =
    project.in(file("bundle/edu.gemini.obslog")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_oodb_auth_servlet =
    project.in(file("bundle/edu.gemini.oodb.auth.servlet")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_oodb_too_url =
    project.in(file("bundle/edu.gemini.oodb.too.url")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_oodb_too_window =
    project.in(file("bundle/edu.gemini.oodb.too.window")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_mail,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_too_event,
      bundle_edu_gemini_util_javax_mail,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_osgi_main =
    project.in(file("bundle/edu.gemini.osgi.main"))

  lazy val bundle_edu_gemini_p2checker =
    project.in(file("bundle/edu.gemini.p2checker")).dependsOn(
      bundle_edu_gemini_ags,
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_jsky_coords
    )

  lazy val bundle_edu_gemini_phase2_core =
    project.in(file("bundle/edu.gemini.phase2.core")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio
    )

  lazy val bundle_edu_gemini_phase2_skeleton_servlet =
    project.in(file("bundle/edu.gemini.phase2.skeleton.servlet")).dependsOn(
      bundle_edu_gemini_auxfile_workflow,
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_phase2_core,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_pot =
    project.in(file("bundle/edu.gemini.pot")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_model_p1_pdf,
      bundle_edu_gemini_model_p1,
      bundle_edu_gemini_util_pdf,
      bundle_jsky_coords,
      bundle_jsky_util
    )

  lazy val bundle_edu_gemini_qpt_client =
    project.in(file("bundle/edu.gemini.qpt.client")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_qpt_shared,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_ui_workspace,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_security_ext,
      bundle_edu_gemini_util_ssh,
      bundle_jsky_coords,
      bundle_jsky_util
    )

  lazy val bundle_edu_gemini_qpt_shared =
    project.in(file("bundle/edu.gemini.qpt.shared")).dependsOn(
      bundle_edu_gemini_ags,
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_coords,
      bundle_jsky_util
    )

  lazy val bundle_edu_gemini_qv_plugin =
    project.in(file("bundle/edu.gemini.qv.plugin")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_horizons_api,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_qpt_shared           % "test->test;compile->compile",
      bundle_edu_gemini_services_client,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_shared_gui,
      bundle_edu_gemini_sp_vcs,
      bundle_edu_gemini_spModel_core         % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_app_ot_plugin,
      bundle_jsky_coords,
      bundle_jsky_elevation_plot
    )

  lazy val bundle_edu_gemini_seqexec_odb =
    project.in(file("bundle/edu.gemini.seqexec.odb")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_spModel_smartgcal,
      bundle_jsky_coords,
      bundle_jsky_util
    )

  lazy val bundle_edu_gemini_services_client =
    project.in(file("bundle/edu.gemini.services.client")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_trpc
    )

  lazy val bundle_edu_gemini_services_server =
    project.in(file("bundle/edu.gemini.services.server")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_services_client,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_skycalc
    )

  lazy val bundle_edu_gemini_shared_ca =
    project.in(file("bundle/edu.gemini.shared.ca")).dependsOn(
    )

  lazy val bundle_edu_gemini_catalog =
    project.in(file("bundle/edu.gemini.catalog")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_spModel_core         % "test->test;compile->compile",
      bundle_jsky_coords,
      bundle_jsky_util,
      bundle_jsky_util_gui,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_shared_gui =
    project.in(file("bundle/edu.gemini.shared.gui")).dependsOn(
      bundle_edu_gemini_util_skycalc
    )

  lazy val bundle_edu_gemini_shared_mail =
    project.in(file("bundle/edu.gemini.shared.mail")).dependsOn(
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_util_javax_mail
    )

  lazy val bundle_edu_gemini_shared_skyobject =
    project.in(file("bundle/edu.gemini.shared.skyobject")).dependsOn(
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_edu_gemini_shared_util =
    project.in(file("bundle/edu.gemini.shared.util"))

  lazy val bundle_edu_gemini_smartgcal_odbinit =
    project.in(file("bundle/edu.gemini.smartgcal.odbinit")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_smartgcal,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_smartgcal_servlet =
    project.in(file("bundle/edu.gemini.smartgcal.servlet")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_smartgcal,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_sp_vcs =
    project.in(file("bundle/edu.gemini.sp.vcs")).dependsOn(
      bundle_edu_gemini_pot % "test->test;compile->compile",
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_sp_vcs_log,
      bundle_edu_gemini_sp_vcs_reg,
      bundle_edu_gemini_spModel_core % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_trpc
    )

  lazy val bundle_edu_gemini_sp_vcs_log =
    project.in(file("bundle/edu.gemini.sp.vcs.log")).dependsOn(
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_sp_vcs_reg =
    project.in(file("bundle/edu.gemini.sp.vcs.reg")).dependsOn(
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_spModel_core =
    project.in(file("bundle/edu.gemini.spModel.core")).dependsOn(
    )

  lazy val bundle_edu_gemini_ags =
    project.in(file("bundle/edu.gemini.ags")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot          % "test->test;compile->compile",
      bundle_edu_gemini_catalog      % "test->test;compile->compile",
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_jsky_coords,
      bundle_jsky_util_gui
    )

  lazy val bundle_edu_gemini_spModel_io =
    project.in(file("bundle/edu.gemini.spModel.io")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core % "test->test;compile->compile",
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_javax_mail
    )

  lazy val bundle_edu_gemini_spModel_pio =
    project.in(file("bundle/edu.gemini.spModel.pio")).dependsOn(
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_shared_util
    )

  lazy val bundle_edu_gemini_spModel_smartgcal =
    project.in(file("bundle/edu.gemini.spModel.smartgcal")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_util_ssl_apache
    )

  lazy val bundle_edu_gemini_spdb_reports_collection =
    project.in(file("bundle/edu.gemini.spdb.reports.collection")).dependsOn(
      bundle_edu_gemini_horizons_api,
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_p2checker,
      bundle_edu_gemini_pot % "test->test;compile->compile",
      bundle_edu_gemini_shared_mail,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_sp_vcs,
      bundle_edu_gemini_sp_vcs_log,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_javax_mail,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_ssh
    )

  lazy val bundle_edu_gemini_spdb_rollover_servlet =
    project.in(file("bundle/edu.gemini.spdb.rollover.servlet")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_edu_gemini_spdb_shell =
    project.in(file("bundle/edu.gemini.spdb.shell")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_security_ext
    )

  lazy val bundle_edu_gemini_too_event =
    project.in(file("bundle/edu.gemini.too.event")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_coords
    )

  lazy val bundle_edu_gemini_ui_workspace =
    project.in(file("bundle/edu.gemini.ui.workspace")).dependsOn(
    )

  lazy val bundle_edu_gemini_util_file_filter =
    project.in(file("bundle/edu.gemini.util.file.filter")).dependsOn(
    )

  lazy val bundle_edu_gemini_util_fits =
    project.in(file("bundle/edu.gemini.util.fits")).dependsOn(
      bundle_edu_gemini_util_file_filter
    )

  lazy val bundle_edu_gemini_util_javax_mail =
    project.in(file("bundle/edu.gemini.util.javax.mail")).dependsOn(
    )

  lazy val bundle_edu_gemini_util_log_extras =
    project.in(file("bundle/edu.gemini.util.log.extras")).dependsOn(
      bundle_edu_gemini_util_javax_mail
    )

  lazy val bundle_edu_gemini_util_osgi =
    project.in(file("bundle/edu.gemini.util.osgi")).dependsOn(
    )

  lazy val bundle_edu_gemini_util_security =
    project.in(file("bundle/edu.gemini.util.security")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio
    )

  lazy val bundle_edu_gemini_util_security_ext =
    project.in(file("bundle/edu.gemini.util.security.ext")).dependsOn(
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_javax_mail,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_util_skycalc =
    project.in(file("bundle/edu.gemini.util.skycalc")).dependsOn(
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_jsky_coords
    )

  lazy val bundle_edu_gemini_util_ssh =
    project.in(file("bundle/edu.gemini.util.ssh")).dependsOn(
    )

  lazy val bundle_edu_gemini_util_ssl =
    project.in(file("bundle/edu.gemini.util.ssl"))

  lazy val bundle_edu_gemini_util_ssl_apache =
    project.in(file("bundle/edu.gemini.util.ssl.apache")).dependsOn(
      bundle_edu_gemini_util_ssl
    )

  lazy val bundle_edu_gemini_util_trpc =
    project.in(file("bundle/edu.gemini.util.trpc")).dependsOn(
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_ssl
    )

  lazy val bundle_edu_gemini_wdba_session_client =
    project.in(file("bundle/edu.gemini.wdba.session.client")).dependsOn(
      bundle_edu_gemini_wdba_shared,
      bundle_edu_gemini_wdba_xmlrpc_api
    )

  lazy val bundle_edu_gemini_wdba_shared =
    project.in(file("bundle/edu.gemini.wdba.shared")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_edu_gemini_wdba_xmlrpc_api =
    project.in(file("bundle/edu.gemini.wdba.xmlrpc.api"))

  lazy val bundle_edu_gemini_wdba_xmlrpc_server =
    project.in(file("bundle/edu.gemini.wdba.xmlrpc.server")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_wdba_shared,
      bundle_edu_gemini_wdba_xmlrpc_api,
      bundle_edu_gemini_util_security
    )

  lazy val bundle_jsky_app_ot_testlauncher =
    project.in(file("bundle/jsky.app.ot.testlauncher")).dependsOn(
      bundle_edu_gemini_qv_plugin,
      bundle_jsky_app_ot,
      bundle_jsky_app_ot_visitlog
    )

  lazy val bundle_jsky_app_ot =
    project.in(file("bundle/jsky.app.ot")).dependsOn(
      bundle_edu_gemini_auxfile_workflow,
      bundle_edu_gemini_ags,
      bundle_edu_gemini_catalog % "test->test;compile->compile",
      bundle_edu_gemini_horizons_api,
      bundle_edu_gemini_itc_shared,
      bundle_edu_gemini_p2checker,
      bundle_edu_gemini_phase2_core,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_gui,
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_sp_vcs,
      bundle_edu_gemini_sp_vcs_log,
      bundle_edu_gemini_sp_vcs_reg,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_spModel_smartgcal,
      bundle_edu_gemini_too_event,
      bundle_edu_gemini_ui_miglayout,
      bundle_edu_gemini_util_javax_mail,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_security_ext,
      bundle_edu_gemini_util_skycalc,
      bundle_edu_gemini_util_ssl,
      bundle_edu_gemini_util_ssl_apache,
      bundle_edu_gemini_util_trpc,
      bundle_edu_gemini_wdba_session_client,
      bundle_edu_gemini_wdba_shared,
      bundle_edu_gemini_wdba_xmlrpc_api,
      bundle_jsky_app_ot_plugin,
      bundle_jsky_app_ot_shared,
      bundle_jsky_coords,
      bundle_jsky_elevation_plot,
      bundle_jsky_util,
      bundle_jsky_util_gui
    )

  lazy val bundle_jsky_app_ot_plugin =
    project.in(file("bundle/jsky.app.ot.plugin")).dependsOn(
      bundle_edu_gemini_ags,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_sp_vcs,
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_jsky_app_ot_shared =
    project.in(file("bundle/jsky.app.ot.shared")).dependsOn(
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_ags,
      bundle_edu_gemini_spModel_io,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_spModel_smartgcal,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_coords
    )

  lazy val bundle_jsky_app_ot_visitlog =
    project.in(file("bundle/jsky.app.ot.visitlog")).dependsOn(
      bundle_edu_gemini_shared_skyobject,
      bundle_edu_gemini_pot,
      bundle_edu_gemini_shared_gui,
      bundle_edu_gemini_shared_util,
      bundle_edu_gemini_sp_vcs,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_spModel_pio,
      bundle_edu_gemini_util_osgi,
      bundle_edu_gemini_util_security,
      bundle_edu_gemini_util_trpc,
      bundle_jsky_app_ot_plugin,
      bundle_jsky_coords
    )

  lazy val bundle_jsky_coords =
    project.in(file("bundle/jsky.coords")).dependsOn(
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_shared_util,
      bundle_jsky_util,
      bundle_jsky_util_gui
    )

  lazy val bundle_jsky_elevation_plot =
    project.in(file("bundle/jsky.elevation.plot")).dependsOn(
      bundle_edu_gemini_catalog,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_util_skycalc,
      bundle_jsky_coords,
      bundle_jsky_util,
      bundle_jsky_util_gui
    )

  lazy val bundle_jsky_util =
    project.in(file("bundle/jsky.util"))

  lazy val bundle_jsky_util_gui =
    project.in(file("bundle/jsky.util.gui")).dependsOn(
      bundle_jsky_util,
      bundle_edu_gemini_util_ssl,
      bundle_edu_gemini_spModel_core,
      bundle_edu_gemini_shared_util
    )

  // From OCS2

  lazy val bundle_edu_gemini_ags_client_api =
    project.in(file("bundle/edu.gemini.ags.client.api")).dependsOn(
      bundle_edu_gemini_model_p1
    )

  lazy val bundle_edu_gemini_ags_client_impl =
    project.in(file("bundle/edu.gemini.ags.client.impl")).dependsOn(
      bundle_edu_gemini_ags_client_api,
      bundle_edu_gemini_model_p1,
      bundle_edu_gemini_util_ssl
    )

  lazy val bundle_edu_gemini_gsa_client =
    project.in(file("bundle/edu.gemini.gsa.client")).dependsOn(
      bundle_edu_gemini_gsa_query,
      bundle_edu_gemini_model_p1
    )

  lazy val bundle_edu_gemini_model_p1 =
    project.in(file("bundle/edu.gemini.model.p1")).dependsOn(
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_edu_gemini_model_p1_pdf =
    project.in(file("bundle/edu.gemini.model.p1.pdf")).dependsOn(
      bundle_edu_gemini_util_pdf
    )

  lazy val bundle_edu_gemini_model_p1_submit =
    project.in(file("bundle/edu.gemini.model.p1.submit")).dependsOn(
      bundle_edu_gemini_model_p1
    )

  lazy val bundle_edu_gemini_model_p1_targetio =
    project.in(file("bundle/edu.gemini.model.p1.targetio")).dependsOn(
      bundle_edu_gemini_model_p1
    )

  lazy val bundle_edu_gemini_p1monitor =
    project.in(file("bundle/edu.gemini.p1monitor")).dependsOn(
      bundle_edu_gemini_model_p1,
      bundle_edu_gemini_model_p1_pdf,
      bundle_edu_gemini_util_pdf,
      bundle_edu_gemini_util_osgi
    )

  lazy val bundle_edu_gemini_pit =
    project.in(file("bundle/edu.gemini.pit")).dependsOn(
      bundle_edu_gemini_ags_client_impl,
      bundle_edu_gemini_gsa_client,
      bundle_edu_gemini_horizons_api,
      bundle_edu_gemini_model_p1,
      bundle_edu_gemini_model_p1_pdf,
      bundle_edu_gemini_model_p1_submit,
      bundle_edu_gemini_model_p1_targetio,
      bundle_edu_gemini_ui_workspace,
      bundle_edu_gemini_shared_gui,
      bundle_edu_gemini_util_pdf,
      bundle_edu_gemini_spModel_core
    )

  lazy val bundle_edu_gemini_pit_launcher =
    project.in(file("bundle/edu.gemini.pit.launcher")).dependsOn(
      bundle_edu_gemini_pit,
      bundle_edu_gemini_ags_client_impl
    )

  lazy val bundle_edu_gemini_tools_p1pdfmaker =
    project.in(file("bundle/edu.gemini.tools.p1pdfmaker")).dependsOn(
      bundle_edu_gemini_model_p1_pdf
    )

  lazy val bundle_edu_gemini_util_pdf =
    project.in(file("bundle/edu.gemini.util.pdf"))

  lazy val bundle_edu_gemini_epics_acm =
    project.in(file("bundle/edu.gemini.epics.acm"))

  lazy val bundle_edu_gemini_ui_miglayout =
    project.in(file("bundle/edu.gemini.ui.miglayout"))

  lazy val bundle_edu_gemini_p1backend =
    project.in(file("bundle/edu.gemini.p1backend")).dependsOn(
      bundle_edu_gemini_model_p1,
      bundle_edu_gemini_model_p1_pdf,
      bundle_edu_gemini_model_p1_submit
    )
}

