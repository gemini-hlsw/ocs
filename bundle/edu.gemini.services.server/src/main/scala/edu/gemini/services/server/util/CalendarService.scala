package edu.gemini.services.server.util


import edu.gemini.spModel.core.Site
import edu.gemini.services.client._
import edu.gemini.services.server.telescope.LttsService
import edu.gemini.spModel.core.osgi.SiteProperty
import java.net.URI
import org.osgi.framework.BundleContext

/**
 * Put together service implementations from available bits and pieces and configure them as needed.
 */
object CalendarService {

  private val GoogleCalendarNorthProp    = "edu.gemini.services.telescope.schedule.id.north"
  private val GoogleCalendarNorthUrlProp = "edu.gemini.services.telescope.schedule.url.north"
  private val GoogleCalendarSouthProp    = "edu.gemini.services.telescope.schedule.id.south"
  private val GoogleCalendarSouthUrlProp = "edu.gemini.services.telescope.schedule.url.south"

  def telescopeScheduleService(ctx: BundleContext, calendarService: CalendarService): TelescopeScheduleService = {

    val site = SiteProperty.get(ctx)
    val lttsService = new LttsService(site)

    val (calendarId, calendarUrl) = site match {

      case Site.GN =>
        require(ctx.getProperty(GoogleCalendarNorthProp) != null,     "google calendar id must be set")
        require(ctx.getProperty(GoogleCalendarNorthUrlProp) != null,  "google calendar url must be set")
        (ctx.getProperty(GoogleCalendarNorthProp), new URI(ctx.getProperty(GoogleCalendarNorthUrlProp)))

      case Site.GS =>
        require(ctx.getProperty(GoogleCalendarSouthProp) != null,     "google calendar id must be set")
        require(ctx.getProperty(GoogleCalendarSouthUrlProp) != null,  "google calendar url must be set")
        (ctx.getProperty(GoogleCalendarSouthProp), new URI(ctx.getProperty(GoogleCalendarSouthUrlProp)))
    }

    new TelescopeScheduleServiceImpl(calendarService, lttsService, calendarId, calendarUrl)
  }

  def calendarService(ctx: BundleContext): CalendarService = {
    val site = SiteProperty.get(ctx)
    new GoogleCalendarService(site)
  }

}