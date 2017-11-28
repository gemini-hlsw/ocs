package edu.gemini.services.server.telescope

import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.calc.Interval
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.{Instant, ZonedDateTime}
import java.time.format.DateTimeFormatter

import edu.gemini.shared.util.DateTimeUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.XML


class LttsService(site: Site) {

  def getNights(range: Interval): Future[Seq[Interval]] =  Future {
    val df = DateTimeUtils.YYYYMMDD_Formatter.withZone(site.timezone.toZoneId)
    val s  = df.format(Instant.ofEpochMilli(range.start))
    val e  = df.format(Instant.ofEpochMilli(range.end))


    val client = new DefaultHttpClient()
    val request = new HttpGet(s"http://$lttsHost:8080/ltts/services/nights?from=$s&to=$e")
    val response = client.execute(request)
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZ")
    val dfp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss.SSSZ")

    // Get the response
    val ltts = XML.load(new InputStreamReader(response.getEntity.getContent))
    val nights = (ltts \\ "night").map(night => {
      val start = ZonedDateTime.parse((night \ "start" text) replaceAllLiterally (":", ""), dfp).toInstant.toEpochMilli
      val end   = ZonedDateTime.parse((night \ "end" text) replaceAllLiterally (":", ""),   dfp).toInstant.toEpochMilli
      Interval(start, end)
    })

    Interval.allDay(nights, site.timezone())

  }

  // TODO: take this from outside configuration..
  private def lttsHost = {
    site match {
      case Site.GN => "gnltts.hi.gemini.edu"
      case Site.GS => "gsltts.cl.gemini.edu"
      case _ => "gnltts.hi.gemini.edu"
    }
  }

}


