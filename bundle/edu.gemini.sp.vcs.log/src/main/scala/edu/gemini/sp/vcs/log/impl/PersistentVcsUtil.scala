package edu.gemini.sp.vcs.log.impl

import scalaz.syntax.std.tuple._
import java.security.MessageDigest

object PersistentVcsUtil {

  import doobie.imports._

  val z = FC.delay("woo")

  implicit class StreamOps[A](stream: Stream[A]) {

    /**
     * Like grouped(n), but grouped using a predicate to determine whether adjacent items are equal. This is properly
     * lazy, so it works fine for inifinite streams.
     */
    def chunked(eq: (A, A) => Boolean): Stream[Stream[A]] = {
      def chunk0(in: Stream[A]): Stream[Stream[A]] = in match {
        case a #:: as     => as.span(eq(_, a)).fold((hs, ts) => (a #:: hs) #:: chunk0(ts))
        case Stream.Empty => Stream.Empty
      }
      chunk0(stream)
    }

  }

  /**
   * Calculate a good hash based on a set of integers. We use this to digest a set of IDs down to a single value.
   */
  def setHash(s:Set[Int]):String = {
    val md = MessageDigest.getInstance("MD5")
    s.toList.sorted.map(n => BigInt(n).toByteArray).foreach(md.update)
    BigInt(1, md.digest).toString(16)
  }


}

