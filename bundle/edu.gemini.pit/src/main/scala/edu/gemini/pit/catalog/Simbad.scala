package edu.gemini.pit.catalog

import edu.gemini.model.p1.{immutable => I}

import java.net.URL
import java.net.URLEncoder.{encode => urlencode}

import edu.gemini.spModel.core._
import votable._
import java.util.UUID

object Simbad extends Catalog with App {

  private lazy val hosts = Array("simbad.u-strasbg.fr", "simbad.cfa.harvard.edu")
  private lazy val simbad = hosts.map(apply).reduceLeft(_ || _)

  def find(id:String)(callback:Result => Unit) {
    simbad.find(id)(callback)
  }

  def apply(host:String):Catalog = new Simbad(host)

  find("sirius") {
    case Success(t, cs) => println((t, cs))
    case x              => println(x)
  }

}

class Simbad private (val host:String) extends VOTableCatalog {

  def url(id:String) = new URL(s"http://$host/simbad/sim-id?output.format=VOTABLE&Ident=${urlencode(id, "UTF-8")}")

  def decode(vot:VOTable):Seq[I.Target] = for {

    // In the List monad here, eventually iterating rows
    resource                         <- vot.resources
    table @ Table("simbad", _, _, _) <- resource.tables
    row                              <- table.data.tableData.rows
    kvs                               = table.fields.zip(row)

    // Local find function
    str                               = (s:String) => kvs.find(_._1.ucd.exists(_.toLowerCase == s.toLowerCase)).map(_._2)
    num                               = (s:String) => str(s).flatMap(_.toDoubleOption)
    // Find magnitudes checking ucd and field name
    magStr                            = (b: String, s:String) => kvs.find(i => i._1.ucd.exists(_.toLowerCase == s.toLowerCase) && i._1.id.endsWith(b)).map(_._2)
    magNum                            = (b: MagnitudeBand, s:String) => magStr(b.name, s).flatMap(_.toDoubleOption)

    // Switch to Option here to pull out data
    epoch                            <- vot.definitions.map(_.cooSys.epoch).map {
                                         case "J2000" => I.CoordinatesEpoch.J_2000
                                         case s       => I.CoordinatesEpoch.forName(s)
                                       }
    name                             <- str("meta.id;meta.main")
    ra                               <- num("pos.eq.ra;meta.main").map(d => RightAscension.fromAngle(Angle.fromDegrees(d)))
    dec                              <- num("pos.eq.dec;meta.main").flatMap(d => Declination.fromAngle(Angle.fromDegrees(d)))

    // Mags get pulled out into a list
    mags                              = for {
                                           (k, Some(v)) <- Map(
                                             MagnitudeBand._u -> magNum(MagnitudeBand._u, "phot.mag;em.opt.U"),
                                             MagnitudeBand._g -> magNum(MagnitudeBand._g, "phot.mag;em.opt.B"),
                                             MagnitudeBand._r -> magNum(MagnitudeBand._r, "phot.mag;em.opt.R"),
                                             MagnitudeBand._i -> magNum(MagnitudeBand._i, "phot.mag;em.opt.I"),
                                             MagnitudeBand._z -> magNum(MagnitudeBand._z, "phot.mag;em.opt.I"),
                                             MagnitudeBand.U  -> magNum(MagnitudeBand.U, "phot.mag;em.opt.U"),
                                             MagnitudeBand.V  -> magNum(MagnitudeBand.V, "phot.mag;em.opt.V"),
                                             MagnitudeBand.B  -> magNum(MagnitudeBand.B, "phot.mag;em.opt.B"),
                                             MagnitudeBand.R  -> magNum(MagnitudeBand.R, "phot.mag;em.opt.R"),
                                             MagnitudeBand.J  -> magNum(MagnitudeBand.J, "phot.mag;em.ir.J"),
                                             MagnitudeBand.H  -> magNum(MagnitudeBand.H, "phot.mag;em.ir.H"),
                                             MagnitudeBand.K  -> magNum(MagnitudeBand.K, "phot.mag;em.ir.K"))
                                         } yield new Magnitude(v, k, k.defaultSystem)

    // Proper Motion
    pm                                = for {
                                          dRa  <- num("pos.pm;pos.eq.ra")
                                          dDec <- num("pos.pm;pos.eq.dec")
                                        } yield I.ProperMotion(dRa, dDec) // TODO: are these correct?

  } yield I.SiderealTarget(UUID.randomUUID(), cleanName(name), Coordinates(ra, dec), epoch, pm, mags.toList)

  private def cleanName(s:String) = if (s.startsWith("NAME ")) s.substring(5) else s

}
