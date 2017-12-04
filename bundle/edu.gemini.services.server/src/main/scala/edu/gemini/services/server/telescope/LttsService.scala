package edu.gemini.services.server.telescope

import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.calc.Interval
import java.io.InputStreamReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import edu.gemini.services.server.util.DateFormatting
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.XML


class LttsService(site: Site) extends DateFormatting {

  def getNights(range: Interval): Future[Seq[Interval]] =  Future {
    val (s,e) = formatStartEndDate(range.start, range.end)

    val client = new DefaultHttpClient()
    val request = new HttpGet(s"http://$lttsHost:8080/ltts/services/nights?from=$s&to=$e")
    val response = client.execute(request)

    // Special formatter that includes the time zone in the specification, and thus does not need a withZone.
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


