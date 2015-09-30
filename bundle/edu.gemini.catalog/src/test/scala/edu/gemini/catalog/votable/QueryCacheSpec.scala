package edu.gemini.catalog.votable

import java.util.concurrent.atomic.AtomicInteger

import org.specs2.mutable.Specification
import ConeSearchBackend.QueryCache

import scala.annotation.tailrec

class QueryCacheSpec extends Specification {
  def factorial(n: Int) = {
    @tailrec
    def go(n: Int, result: Int): Int =
      if (n == 0) {
        result
      } else {
        go(n-1, result * n)
      }

    go(n, 1)
  }

  "QueryCache Spec" should {
    "not cache if contains does not find anything" in {
      def contains(a: QueryCache.CacheContainer[Int, Int], k: Int):Option[(Int, Int)] = {
        None
      }
      val callCounter = new AtomicInteger(0)
      def factorialCounter(n: Int) = {
        callCounter.incrementAndGet()
        factorial(n)
      }
      val memo = QueryCache.buildCache(contains)(factorialCounter)
      val answers = for {
          i <- 0 to 10
        } yield memo(i)
      answers(10) should beEqualTo(3628800)
      callCounter.get() should beEqualTo(11)
    }
    "cache already called values" in {
      def contains(a: QueryCache.CacheContainer[Int, Int], k: Int):Option[(Int, Int)] = {
        a.find(_.k == k).map(i => (k, i.v))
      }
      val callCounter = new AtomicInteger(0)
      def factorialCounter(n: Int) = {
        callCounter.incrementAndGet()
        factorial(n)
      }
      val memo = QueryCache.buildCache(contains)(factorialCounter)

      // prime the cache
      val answers = for {
          i <- 0 to 10
        } yield memo(i)
      answers(10) should beEqualTo(3628800)
      // This should come from the cache
      memo(10) should beEqualTo(3628800)
      callCounter.get should beEqualTo(11)
    }
    "forget older values" in {
      def contains(a: QueryCache.CacheContainer[Int, Int], k: Int):Option[(Int, Int)] = {
        a.find(_.k == k).map(i => (k, i.v))
      }
      val callCounter = new AtomicInteger(0)
      def factorialCounter(n: Int) = {
        callCounter.incrementAndGet()
        factorial(n)
      }
      val memo = QueryCache.buildCache(contains)(factorialCounter)

      for {
          i <- 0 to 100
        } yield memo(i)

      callCounter.get should beEqualTo(101)
      // Element 10 sholud still be in the cache
      memo(10) should beEqualTo(3628800)
      callCounter.get should beEqualTo(101)
      // The cache should have forgotten element 0, so it will recalculate
      memo(0) should beEqualTo(1)
      callCounter.get should beEqualTo(102)
    }
    "be performant" in {
      skipped("Used only for performance checks")
      def contains(a: QueryCache.CacheContainer[Int, Int], k: Int):Option[(Int, Int)] = {
        a.find(_.k == k).map(i => (k, i.v))
      }

      // Performance is linear with maxSize, for the typical case of 100 it is to fast to measure
      val maxSize = 10000
      val memo = QueryCache.buildCache(contains, maxSize)(factorial)

      val s = System.currentTimeMillis()

      val hits = 100000
      for {
          i <- 0 to hits
        } yield memo(i)

      // About 19 seconds using mutable ListBuffer
      // About 20 seconds using mutable ArrayBuffer
      // About 380 seconds using LinkedList
      // About 46 seconds using MutableList
      // About 18 seconds using List
      // About 17 seconds using Vector
      val time = System.currentTimeMillis() - s

      time must be_>=(0L)
    }
  }
}
