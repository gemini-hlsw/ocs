package edu.gemini.util.skycalc.calc

import org.junit.Test
import org.junit.Assert._

class SolutionTest {

  @Test def add(): Unit = {
    val s1 = Solution(Interval(1,2))
    val s2 = Solution(Interval(2,5))
    val s3 = Solution(Interval(8,9))

    assertEquals(Solution(Interval(1,5)), s1.add(s2))
    assertEquals(Solution(Seq(Interval(1,5), Interval(8,9))), s1.add(s2).add(s3))
  }

  @Test def combine(): Unit = {
    val s1 = Solution(Seq(Interval(1,2), Interval(5,9)))
    val s2 = Solution(Seq(Interval(2,3), Interval(4,8), Interval(11,14)))
    val result = Solution(Seq(Interval(1,3), Interval(4,9), Interval(11,14)))

    assertEquals(result, s1.combine(s2))
    assertEquals(result, s2.combine(s1))
  }

  @Test def intersect(): Unit = {
    val s1 = Solution(Seq(Interval(1,2), Interval(5,9)))
    val s2 = Solution(Seq(Interval(2,3), Interval(4,8), Interval(11,14)))
    val result = Solution(Interval(5,8))

    assertEquals(result, s1.intersect(s2))
    assertEquals(result, s2.intersect(s1))
  }

  @Test def simpleReduce(): Unit = {
    val s = Solution(Seq(Interval(10,20)))

    val s1 = Solution()
    val s2 = Solution(Seq(Interval(5,6)))
    val s3 = Solution(Seq(Interval(5,15)))
    val s4 = Solution(Seq(Interval(12,18)))
    val s5 = Solution(Seq(Interval(15,25)))
    val s6 = Solution(Seq(Interval(25,30)))
    val s7 = Solution(Seq(Interval(5, 25)))

    assertEquals(s, s.reduce(s1))
    assertEquals(s, s.reduce(s2))
    assertEquals(Solution(Seq(Interval(15, 20))), s.reduce(s3))
    assertEquals(Solution(Seq(Interval(10, 12), Interval(18, 20))), s.reduce(s4))
    assertEquals(Solution(Seq(Interval(10, 15))), s.reduce(s5))
    assertEquals(s, s.reduce(s6))
    assertEquals(Solution(), s.reduce(s7))
  }

  @Test def emptyReduce(): Unit = {
    val empty = Solution()
    val s1 = Solution(Seq(Interval(1,10)))
    val s2 = Solution(Seq(Interval(5,6), Interval(9,12)))

    assertEquals(empty, empty.reduce(s1))
    assertEquals(empty, empty.reduce(s2))
    assertEquals(s1, s1.reduce(empty))
    assertEquals(s2, s2.reduce(empty))
  }

  @Test def complexReduce(): Unit = {
    val s1 = Solution(Seq(Interval(0,10), Interval(20,30), Interval(40,50), Interval(60,70)))
    val s2 = Solution(Seq(Interval(5,6), Interval(15,35), Interval(45,55)))

    assertEquals(Solution(Seq(Interval(0,5),Interval(6,10),Interval(40,45),Interval(60,70))), s1.reduce(s2))
    assertEquals(Solution(Seq(Interval(15,20),Interval(30,35),Interval(50,55))), s2.reduce(s1))
  }

  @Test def complexReduce2(): Unit = {
    val s1 = Solution(Seq(Interval(0,10), Interval(20,30), Interval(40,50), Interval(60,70)))
    val s2 = Solution(Seq(Interval(5,25), Interval(55,100)))

    assertEquals(Solution(Seq(Interval(0,5),Interval(25,30),Interval(40,50))), s1.reduce(s2))
    assertEquals(Solution(Seq(Interval(10,20),Interval(55,60),Interval(70,100))), s2.reduce(s1))
  }

}
