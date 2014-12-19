package edu.gemini.pit.catalog

import edu.gemini.model.p1.{ immutable => I, mutable => M }

import java.net.URL
import java.net.URLEncoder.{ encode => urlencode }

import votable._
import java.util.UUID

object Ned extends Catalog {

  private lazy val host = "ned.ipac.caltech.edu"
  private lazy val ned = Ned(host) // hosts.map(apply).reduceLeft(_ || _)

  def find(id: String)(callback: Result => Unit) {
    ned.find(id)(callback)
  }

  def apply(host: String): Catalog = new Ned(host)

}

class Ned private (val host: String) extends VOTableCatalog {

  val urlTemplate = "http://%s/cgi-bin/nph-objsearch?objname=%s&extend=no&hconst=73&omegam=0.27&omegav=0.73&corr_z=1&out_csys=Equatorial&out_equinox=J2000.0&obj_sort=RA+or+Longitude&of=xml_main&zv_breaker=30000.0&list_limit=5&img_stamp=YES"

  def url(id: String) = new URL(urlTemplate.format(host, urlencode(id, "UTF-8")))

  def decode(vot: VOTable): Seq[I.Target] = for {

    // In the List monad here, eventually iterating rows
    resource <- vot.resources
    table @ Table("NED_MainTable", _, _, _) <- resource.tables
    row <- table.data.tableData.rows
    kvs = table.fields.zip(row)

    // Local find function
    find = (s: String) => kvs.find(_._1.ucd == Some(s)).map(_._2)

    // Switch to Option here to pull out data
    epoch <- vot.definitions.map(_.cooSys.id) collect {
      case "J2000" => M.CoordinatesEpoch.J_2000
    }
    name <- find("meta.id;meta.main")
    ra <- find("pos.eq.ra;meta.main").flatMap(_.toDoubleOption)
    dec <- find("pos.eq.dec;meta.main").flatMap(_.toDoubleOption)

  } yield I.SiderealTarget(UUID.randomUUID(), name, I.DegDeg(ra, dec), epoch, None, List())

}

