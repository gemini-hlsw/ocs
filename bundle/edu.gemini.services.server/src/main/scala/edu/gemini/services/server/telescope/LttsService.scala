package edu.gemini.services.server.telescope

import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.calc.Interval
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.XML


class LttsService(site: Site) {

  def getNights(range: Interval): Future[Seq[Interval]] =  Future {

    val s = TimeUtils.print(range.start, site.timezone, "yyyyMMdd")
    val e = TimeUtils.print(range.end, site.timezone, "yyyyMMdd")


    val client = new DefaultHttpClient()
    val request = new HttpGet(s"http://$lttsHost:8080/ltts/services/nights?from=$s&to=$e")
    val response = client.execute(request)
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZ")

    // Get the response
    val ltts = XML.load(new InputStreamReader(response.getEntity().getContent()))
    val nights = (ltts \\ "night").map(night => {
      val start = sdf.parse((night \ "start" text) replaceAllLiterally (":", "")).getTime
      val end   = sdf.parse((night \ "end" text) replaceAllLiterally (":", "")).getTime
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


