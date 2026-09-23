package edu.gemini.itc.web.perf

import argonaut._, Argonaut._
import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.web.golden.{GoldenCase, GoldenCases}

import java.io.{File, PrintWriter}
import java.lang.management.ManagementFactory
import scala.io.Source

/**
 * Times the ITC on the golden cases and reports wall time and bytes allocated per call.
 *
 * Run from sbt (forked, so pass options as arguments):
 *   test:runMain edu.gemini.itc.web.perf.ItcPerfHarness --out before.json
 *   test:runMain edu.gemini.itc.web.perf.ItcPerfHarness --out after.json --compare before.json
 *
 * Options: --warmup N (default 5), --iter N (default 20), --filter text, --out file, --compare file.
 * Allocation is measured on the calling thread and is deterministic, so it is the most reliable
 * before and after signal. Wall time is the median of the measured iterations.
 */
object ItcPerfHarness {

  final case class Sample(medianMs: Double, p90Ms: Double, minMs: Double, allocMb: Double)

  private val threads = ManagementFactory.getThreadMXBean.asInstanceOf[com.sun.management.ThreadMXBean]

  private def allocated(): Long = threads.getThreadAllocatedBytes(Thread.currentThread.getId)

  def measure(c: GoldenCase, warmup: Int, iter: Int): Sample = {
    val itc = new ItcServiceImpl
    def once(): Unit =
      itc.calculate(c.params, false).fold(e => sys.error(s"${c.key}: ITC error: ${e.msg}"), _ => ())

    (1 to warmup).foreach(_ => once())

    val times  = new Array[Double](iter)
    val allocs = new Array[Double](iter)
    var i = 0
    while (i < iter) {
      val a0 = allocated()
      val t0 = System.nanoTime()
      once()
      times(i)  = (System.nanoTime() - t0) / 1e6
      allocs(i) = (allocated() - a0) / (1024.0 * 1024.0)
      i += 1
    }
    java.util.Arrays.sort(times)
    java.util.Arrays.sort(allocs)
    Sample(times(iter / 2), times(math.min(iter - 1, (iter * 0.9).toInt)), times(0), allocs(iter / 2))
  }

  private def usedHeapMb(): Double = {
    System.gc(); System.gc()
    val rt = Runtime.getRuntime
    (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0)
  }

  // ===== JSON in and out

  private def toJson(rs: List[(String, Sample)]): Json =
    Json.obj(rs.map { case (k, s) =>
      k -> Json("medianMs" := s.medianMs, "p90Ms" := s.p90Ms, "minMs" := s.minMs, "allocMb" := s.allocMb)
    }: _*)

  private def fromJson(file: File): Map[String, Sample] = {
    val src = Source.fromFile(file, "UTF-8")
    val text = try src.mkString finally src.close()
    def d(j: Json, f: String) = j.fieldOrNull(f).number.flatMap(_.toDouble).getOrElse(Double.NaN)
    Parse.parse(text).fold(e => sys.error(s"cannot parse $file: $e"), identity).objectOrEmpty.toList.map { case (k, j) =>
      k -> Sample(d(j, "medianMs"), d(j, "p90Ms"), d(j, "minMs"), d(j, "allocMb"))
    }.toMap
  }

  // ===== Reporting

  private def pct(before: Double, after: Double): String =
    if (before == 0.0 || before.isNaN) "   n/a" else f"${(after - before) / before * 100}%+6.1f%%"

  private def print(rs: List[(String, Sample)], before: Option[Map[String, Sample]]): Unit = {
    val w   = rs.map(_._1.length).max
    val key = "%-" + w + "s"
    def row(k: String, cols: String*): Unit = println((key +: cols.map(_ => "  %10s")).mkString.format(k +: cols: _*))
    def ms(d: Double) = f"$d%.1f"
    before match {
      case None =>
        row("case", "median ms", "p90 ms", "min ms", "alloc MB")
        rs.foreach { case (k, s) => row(k, ms(s.medianMs), ms(s.p90Ms), ms(s.minMs), ms(s.allocMb)) }
      case Some(b) =>
        row("case", "ms before", "ms after", "delta", "MB before", "MB after", "delta")
        rs.foreach { case (k, s) =>
          val o = b.getOrElse(k, Sample(Double.NaN, Double.NaN, Double.NaN, Double.NaN))
          row(k, ms(o.medianMs), ms(s.medianMs), pct(o.medianMs, s.medianMs), ms(o.allocMb), ms(s.allocMb), pct(o.allocMb, s.allocMb))
        }
        val matched = rs.filter(r => b.contains(r._1))
        val (tb, ta) = (matched.map(r => b(r._1).medianMs).sum, matched.map(_._2.medianMs).sum)
        val (ab, aa) = (matched.map(r => b(r._1).allocMb).sum, matched.map(_._2.allocMb).sum)
        row("total", ms(tb), ms(ta), pct(tb, ta), ms(ab), ms(aa), pct(ab, aa))
    }
  }

  def main(args: Array[String]): Unit = {
    val opts = args.sliding(2, 2).collect { case Array(k, v) if k.startsWith("--") => k.drop(2) -> v }.toMap
    val warmup  = opts.get("warmup").map(_.toInt).getOrElse(5)
    val iter    = opts.get("iter").map(_.toInt).getOrElse(20)
    val filter  = opts.getOrElse("filter", "")
    val cases   = GoldenCases.all.filter(_.key.contains(filter))
    require(cases.nonEmpty, s"no case matches '$filter'")

    println(s"Java ${System.getProperty("java.version")}, max heap ${Runtime.getRuntime.maxMemory() / (1024 * 1024)} MB, warmup $warmup, iterations $iter")
    val heapBefore = usedHeapMb()

    val results = cases.map { c =>
      System.err.println(s"measuring ${c.key}")
      c.key -> measure(c, warmup, iter)
    }

    print(results, opts.get("compare").map(f => fromJson(new File(f))))
    println(f"retained heap after gc: before ${heapBefore}%.1f MB, after ${usedHeapMb()}%.1f MB (mostly the data file cache)")

    opts.get("out").foreach { f =>
      val w = new PrintWriter(new File(f), "UTF-8")
      try w.write(toJson(results).spaces2) finally w.close()
      println(s"wrote $f")
    }
  }

}
