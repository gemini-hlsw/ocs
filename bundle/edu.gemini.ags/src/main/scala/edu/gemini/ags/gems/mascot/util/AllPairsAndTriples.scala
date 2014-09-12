package edu.gemini.ags.gems.mascot.util

/**
 * A general purpose utility to find all unique pairs or triplets in a list.
 *
 * @author Shane Walker
 */

object AllPairsAndTriples {

  private def allTrips2[T](lst: List[T]): List[(T, T, T)] = {
    val s = lst.size
    val lz = lst.zipWithIndex

    for {
      (x, i) <- lz.take(s - 2)
      (y, j) <- lz.slice(i + 1, s - 1)
      z <- lst.drop(j + 1)
    } yield (x, y, z)
  }

  def allTrips[T](l: List[T]): List[(T, T, T)] =
    if (l.size < 3) Nil else allTrips2(l)

  private def allPairs2[T](lst: List[T]): List[(T, T)] = {
    val s = lst.size
    val lz = lst.zipWithIndex

    for {
      (x, i) <- lz.take(s - 1)
      y <- lst.slice(i + 1, s)
    } yield (x, y)
  }

  def allPairs[T](l: List[T]): List[(T, T)] =
    if (l.size < 2) Nil else allPairs2(l)

  def main(args: Array[String]) {
    val l = List("a", "b", "c", "d", "e")
    println(allPairs(l))
    println(allTrips(l))

    println(allTrips(List("x", "y", "z")))
    println(allTrips(List(1, 2, 3, 4)))
  }
}

