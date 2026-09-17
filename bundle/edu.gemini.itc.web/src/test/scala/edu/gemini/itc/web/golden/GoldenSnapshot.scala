package edu.gemini.itc.web.golden

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

import java.io.{File, FileInputStream, FileOutputStream, OutputStreamWriter}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.io.Source

/** One chart series: the x axis is regular so only its ends are kept, y is kept in full. */
final case class GoldenSeries(name: String, xStart: Double, xEnd: Double, y: Array[Double])

/** Everything numeric that an ITC result carries, flattened so it can be diffed value by value. */
final case class GoldenSnapshot(scalars: List[(String, Double)], strings: List[(String, String)], series: List[GoldenSeries])

final case class Mismatch(where: String, expected: String, actual: String, relDiff: Double)

object GoldenSnapshot {

  def of(r: ItcResult): GoldenSnapshot = {
    val scalars = List.newBuilder[(String, Double)]
    val strings = List.newBuilder[(String, String)]
    val series  = List.newBuilder[GoldenSeries]

    def ccds(cs: List[ItcCcd]): Unit = cs.zipWithIndex.foreach { case (c, i) =>
      scalars += s"ccd.$i.singleSNRatio" -> c.singleSNRatio
      scalars += s"ccd.$i.totalSNRatio"  -> c.totalSNRatio
      scalars += s"ccd.$i.peakPixelFlux" -> c.peakPixelFlux
      scalars += s"ccd.$i.wellDepth"     -> c.wellDepth
      scalars += s"ccd.$i.ampGain"       -> c.ampGain
      c.warnings.zipWithIndex.foreach { case (w, j) => strings += s"ccd.$i.warning.$j" -> w.msg }
    }

    def times(t: AllIntegrationTimes): Unit = {
      scalars += "times.selectedIndex" -> t.selectedIndex.toDouble
      t.detectors.zipWithIndex.foreach { case (d, i) =>
        scalars += s"times.$i.exposureTime" -> d.exposureTime
        scalars += s"times.$i.exposures"    -> d.exposures.toDouble
      }
    }

    def snAt(s: Option[SignalToNoiseAt]): Unit = s.foreach { a =>
      scalars += "snAt.wavelength" -> a.wavelength
      scalars += "snAt.single"     -> a.singleSignalToNoise
      scalars += "snAt.final"      -> a.finalSignalToNoise
    }

    r match {
      case i: ItcImagingResult =>
        ccds(i.ccds); times(i.times); snAt(i.snAt)
      case s: ItcSpectroscopyResult =>
        ccds(s.ccds); times(s.times); snAt(s.snAt)
        s.chartGroups.zipWithIndex.foreach { case (g, gi) =>
          g.charts.zipWithIndex.foreach { case (c, ci) =>
            c.series.foreach { d =>
              val xs = d.xValues
              val (x0, x1) = if (xs.isEmpty) (Double.NaN, Double.NaN) else (xs.head, xs.last)
              series += GoldenSeries(s"g$gi/c$ci:${c.chartType}/${d.title}", x0, x1, d.yValues.clone())
            }
          }
        }
    }
    GoldenSnapshot(scalars.result(), strings.result(), series.result())
  }

  // ===== Comparison

  // NaN only matches NaN and an infinity only matches the same infinity; the tolerance applies to finite values
  private def close(a: Double, b: Double, relTol: Double): Boolean =
    if (a.isNaN || b.isNaN) a.isNaN && b.isNaN
    else if (a.isInfinite || b.isInfinite) a == b
    else a == b || math.abs(a - b) <= relTol * math.max(math.abs(a), math.abs(b))

  private def rel(a: Double, b: Double): Double =
    if (a.isNaN || b.isNaN || a.isInfinite || b.isInfinite) { if (close(a, b, 0.0)) 0.0 else Double.PositiveInfinity }
    else {
      val m = math.max(math.abs(a), math.abs(b))
      if (m == 0.0) 0.0 else math.abs(a - b) / m
    }

  /** Compares an expected snapshot with an actual one. Empty result means they agree within `relTol`. */
  def compare(expected: GoldenSnapshot, actual: GoldenSnapshot, relTol: Double): List[Mismatch] = {
    val out = List.newBuilder[Mismatch]

    def num(where: String, e: Double, a: Double): Unit =
      if (!close(e, a, relTol)) out += Mismatch(where, e.toString, a.toString, rel(e, a))

    val actScalars = actual.scalars.toMap
    expected.scalars.foreach { case (k, e) =>
      actScalars.get(k).fold[Unit](out += Mismatch(k, e.toString, "<missing>", Double.NaN))(a => num(k, e, a))
    }
    (actScalars.keySet -- expected.scalars.map(_._1)).toList.sorted.foreach { k =>
      out += Mismatch(k, "<missing>", actScalars(k).toString, Double.NaN)
    }

    val actStrings = actual.strings.toMap
    expected.strings.foreach { case (k, e) =>
      val a = actStrings.getOrElse(k, "<missing>")
      if (a != e) out += Mismatch(k, e, a, Double.NaN)
    }
    (actStrings.keySet -- expected.strings.map(_._1)).toList.sorted.foreach { k =>
      out += Mismatch(k, "<missing>", actStrings(k), Double.NaN)
    }

    val actSeries = actual.series.map(s => s.name -> s).toMap
    expected.series.foreach { e =>
      actSeries.get(e.name) match {
        case None => out += Mismatch(e.name, s"${e.y.length} points", "<missing>", Double.NaN)
        case Some(a) =>
          num(s"${e.name}.xStart", e.xStart, a.xStart)
          num(s"${e.name}.xEnd",   e.xEnd,   a.xEnd)
          if (e.y.length != a.y.length)
            out += Mismatch(s"${e.name}.length", e.y.length.toString, a.y.length.toString, Double.NaN)
          else {
            var i = 0
            while (i < e.y.length) {
              num(s"${e.name}[$i]", e.y(i), a.y(i))
              i += 1
            }
          }
      }
    }
    (actSeries.keySet -- expected.series.map(_.name)).toList.sorted.foreach { k =>
      out += Mismatch(k, "<missing>", s"${actSeries(k).y.length} points", Double.NaN)
    }

    out.result()
  }

  def report(key: String, ms: List[Mismatch], maxShown: Int = 10): String = {
    val (series, scalars) = ms.partition(_.where.contains("["))
    val head = s"$key: ${ms.size} mismatches (${scalars.size} scalars, ${series.size} series values)"

    def line(m: Mismatch) = {
      val r = if (m.relDiff.isNaN) "" else f" (rel ${m.relDiff}%.3e)"
      s"  ${m.where}: expected ${m.expected}, got ${m.actual}$r"
    }

    val scalarLines = scalars.sortBy(m => -m.relDiff).take(maxShown).map(line)

    val seriesLines = {
      val rel = series.map(_.relDiff).filterNot(_.isNaN).sorted
      if (rel.isEmpty) Nil
      else {
        def pct(p: Double) = rel(math.min(rel.length - 1, (rel.length * p).toInt))
        val worst = series.maxBy(m => if (m.relDiff.isNaN) -1.0 else m.relDiff)
        List(
          f"  series values: median rel ${pct(0.5)}%.3e, p90 ${pct(0.9)}%.3e, p99 ${pct(0.99)}%.3e, max ${rel.last}%.3e, ${rel.count(_ > 1e-3)} above 1e-3",
          "  worst" + line(worst).drop(1)
        )
      }
    }

    val more = if (scalars.size > maxShown) List(s"  ... ${scalars.size - maxShown} more scalars") else Nil
    (head :: scalarLines ::: seriesLines ::: more).mkString("\n")
  }

  // ===== JSON

  private def num(d: Double): Json = jNumberOrString(d)

  private def dbl(j: Json): Double =
    j.number.flatMap(_.toDouble).orElse(j.string.map(_.toDouble)).getOrElse(sys.error(s"not a number: $j"))

  private def seriesJson(s: GoldenSeries): Json = Json(
    "name"   := s.name,
    "xStart" -> num(s.xStart),
    "xEnd"   -> num(s.xEnd),
    "y"      -> jArray(s.y.toList.map(num))
  )

  private def seriesFrom(j: Json): GoldenSeries = {
    val f = j.fieldOrEmptyString _
    GoldenSeries(
      f("name").stringOrEmpty,
      dbl(f("xStart")),
      dbl(f("xEnd")),
      f("y").arrayOrEmpty.map(dbl).toArray
    )
  }

  def toJson(s: GoldenSnapshot): Json = Json(
    "scalars" -> Json.obj(s.scalars.map { case (k, v) => k -> num(v) }: _*),
    "strings" -> Json.obj(s.strings.map { case (k, v) => k -> jString(v) }: _*),
    "series"  -> jArray(s.series.map(seriesJson))
  )

  def fromJson(j: Json): GoldenSnapshot = {
    val f = j.fieldOrEmptyString _
    GoldenSnapshot(
      f("scalars").objectOrEmpty.toList.map { case (k, v) => k -> dbl(v) },
      f("strings").objectOrEmpty.toList.map { case (k, v) => k -> v.stringOrEmpty },
      f("series").arrayOrEmpty.map(seriesFrom)
    )
  }

  // ===== Golden file, a gzipped JSON object keyed by case

  def write(file: File, all: Map[String, GoldenSnapshot]): Unit = {
    file.getParentFile.mkdirs()
    val w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), "UTF-8")
    try w.write(Json.obj(all.toList.sortBy(_._1).map { case (k, s) => k -> toJson(s) }: _*).nospaces)
    finally w.close()
  }

  def read(file: File): Map[String, GoldenSnapshot] = {
    val in   = new GZIPInputStream(new FileInputStream(file))
    val text = try Source.fromInputStream(in, "UTF-8").mkString finally in.close()
    Parse.parse(text).fold(e => sys.error(s"cannot parse $file: $e"), identity)
      .objectOrEmpty.toList.map { case (k, v) => k -> fromJson(v) }.toMap
  }

}
