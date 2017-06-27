
import sbt.{ State => _, Configuration => _, Show => _, _ }
import Keys._

trait OcsApp { this: OcsBundle =>

  lazy val app_bundle_server = project.in(file("app/bundle-server"))

  lazy val app_epics_acm = project.in(file("app/epics-acm")).dependsOn(
    bundle_edu_gemini_epics_acm
  )

  lazy val app_itc = project.in(file("app/itc")).dependsOn(
    bundle_edu_gemini_itc
  )

  lazy val app_pit = project.in(file("app/pit")).dependsOn(
    bundle_edu_gemini_pit
  )

  lazy val app_p1pdfmaker = project.in(file("app/p1pdfmaker")).dependsOn(
    bundle_edu_gemini_tools_p1pdfmaker
  )

  lazy val app_p1_monitor = project.in(file("app/p1-monitor")).dependsOn(
    bundle_edu_gemini_p1monitor,
    bundle_edu_gemini_util_javax_mail
  )

  lazy val app_ot = project.in(file("app/ot")).dependsOn(
    bundle_edu_gemini_sp_vcs,
    bundle_edu_gemini_sp_vcs_log,
    bundle_edu_gemini_sp_vcs_reg,
    bundle_edu_gemini_spdb_shell,
    bundle_edu_gemini_util_log_extras,
    bundle_jsky_app_ot,
    bundle_jsky_app_ot_visitlog,
    bundle_edu_gemini_qv_plugin
  )

  lazy val app_qpt = project.in(file("app/qpt")).dependsOn(
    bundle_edu_gemini_shared_gui,
    bundle_edu_gemini_util_log_extras,
    bundle_edu_gemini_ags,
    bundle_edu_gemini_spModel_io,
    bundle_edu_gemini_spModel_smartgcal,
    bundle_edu_gemini_qpt_client
  )

  lazy val app_spdb = project.in(file("app/spdb")).dependsOn(
    bundle_edu_gemini_catalog,
    bundle_edu_gemini_p2checker,
    bundle_edu_gemini_spdb_shell,
    bundle_edu_gemini_util_log_extras,
    bundle_edu_gemini_lchquery_servlet,
    bundle_edu_gemini_qpt_shared,
    bundle_jsky_app_ot_shared,
    bundle_edu_gemini_sp_vcs,
    bundle_edu_gemini_sp_vcs_log,
    bundle_edu_gemini_sp_vcs_reg,
    bundle_edu_gemini_phase2_skeleton_servlet,
    bundle_edu_gemini_spdb_rollover_servlet,
    bundle_edu_gemini_ags_servlet,
    bundle_edu_gemini_ags,
    bundle_edu_gemini_dataman_app,
    bundle_edu_gemini_oodb_auth_servlet,
    bundle_edu_gemini_oodb_too_url,
    bundle_edu_gemini_oodb_too_window,
    bundle_edu_gemini_smartgcal_odbinit,
    bundle_edu_gemini_util_security_ext,
    bundle_edu_gemini_wdba_session_client,
    bundle_edu_gemini_wdba_xmlrpc_server,
    bundle_edu_gemini_obslog,
    bundle_edu_gemini_services_server,
    bundle_edu_gemini_smartgcal_servlet,
    bundle_edu_gemini_itc
  )

  lazy val app_weather = project.in(file("app/weather")).dependsOn(
    bundle_edu_gemini_shared_ca,
    bundle_edu_gemini_spdb_reports_collection
  )

  lazy val app_all_bundles = project.in(file("app/all-bundles")).dependsOn(
    bundle_edu_gemini_ags,
    bundle_edu_gemini_ags_servlet,
    bundle_edu_gemini_auxfile_workflow,
    bundle_edu_gemini_catalog,
    bundle_edu_gemini_dataman_app,
    bundle_edu_gemini_horizons_api,
    bundle_edu_gemini_lchquery_servlet,
    bundle_edu_gemini_obslog,
    bundle_edu_gemini_oodb_auth_servlet,
    bundle_edu_gemini_oodb_too_url,
    bundle_edu_gemini_oodb_too_window,
    bundle_edu_gemini_osgi_main,
    bundle_edu_gemini_p2checker,
    bundle_edu_gemini_phase2_core,
    bundle_edu_gemini_phase2_skeleton_servlet,
    bundle_edu_gemini_pot,
    bundle_edu_gemini_qpt_client,
    bundle_edu_gemini_qpt_server,
    bundle_edu_gemini_qpt_shared,
    bundle_edu_gemini_qv_plugin,
    bundle_edu_gemini_seqexec_odb,
    bundle_edu_gemini_services_client,
    bundle_edu_gemini_services_server,
    bundle_edu_gemini_shared_ca,
    bundle_edu_gemini_shared_gui,
    bundle_edu_gemini_shared_mail,
    bundle_edu_gemini_shared_skyobject,
    bundle_edu_gemini_shared_util,
    bundle_edu_gemini_smartgcal_odbinit,
    bundle_edu_gemini_smartgcal_servlet,
    bundle_edu_gemini_sp_vcs,
    bundle_edu_gemini_sp_vcs_log,
    bundle_edu_gemini_sp_vcs_reg,
    bundle_edu_gemini_spModel_core,
    bundle_edu_gemini_ags,
    bundle_edu_gemini_spModel_io,
    bundle_edu_gemini_spModel_pio,
    bundle_edu_gemini_spModel_smartgcal,
    bundle_edu_gemini_spdb_reports_collection,
    bundle_edu_gemini_spdb_rollover_servlet,
    bundle_edu_gemini_spdb_shell,
    bundle_edu_gemini_too_event,
    bundle_edu_gemini_ui_workspace,
    bundle_edu_gemini_ui_miglayout,
    bundle_edu_gemini_util_file_filter,
    bundle_edu_gemini_util_fits,
    bundle_edu_gemini_util_javax_mail,
    bundle_edu_gemini_util_log_extras,
    bundle_edu_gemini_util_osgi,
    bundle_edu_gemini_util_security,
    bundle_edu_gemini_util_security_ext,
    bundle_edu_gemini_util_skycalc,
    bundle_edu_gemini_util_ssh,
    bundle_edu_gemini_util_ssl,
    bundle_edu_gemini_util_ssl_apache,
    bundle_edu_gemini_util_trpc,
    bundle_edu_gemini_wdba_session_client,
    bundle_edu_gemini_wdba_shared,
    bundle_edu_gemini_wdba_xmlrpc_api,
    bundle_edu_gemini_wdba_xmlrpc_server,
    bundle_edu_gemini_pit,
    bundle_edu_gemini_pit_launcher,
    bundle_edu_gemini_p1monitor,
    bundle_edu_gemini_tools_p1pdfmaker,
    bundle_jsky_app_ot,
    bundle_jsky_app_ot_plugin,
    bundle_jsky_app_ot_shared,
    bundle_jsky_app_ot_testlauncher,
    bundle_jsky_app_ot_visitlog,
    bundle_jsky_coords,
    bundle_jsky_elevation_plot,
    bundle_jsky_util,
    bundle_jsky_util_gui
  )

}

