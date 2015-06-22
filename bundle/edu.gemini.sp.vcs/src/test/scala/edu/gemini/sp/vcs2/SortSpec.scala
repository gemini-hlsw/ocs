package edu.gemini.sp.vcs2

import org.scalacheck.Prop.forAll
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object SortSpec extends Specification with ScalaCheck {
  case class Test(merged: List[Char], preferred: List[Char], alternate: List[Char]) {
    val sorted = SortHeuristic.sort(merged, preferred, alternate)(identity)

    override def toString: String =
      s"""----------
         |Merged...: ${merged.mkString}
         |Preferred: ${preferred.mkString}
         |Alternate: ${alternate.mkString}
         |Sorted...: ${sorted.mkString}
       """.stripMargin
  }

  val genTest = for {
    p <- Gen.listOf(Gen.alphaChar)
    a <- Gen.listOf(Gen.alphaChar)
    m <- Gen.someOf(p ++ a)
  } yield Test(m.toList.distinct, p.toList.distinct, a.toList.distinct)

  // checks whether l is in order with respect to orderedList and contains no
  // characters not contained in orderedList (though orderedList might contain
  // characters not in l)
  def isOrdered(l: List[Char], orderedList: List[Char]): Boolean = {
    val order   = orderedList.zipWithIndex.toMap
    val indices = l.map(c => order.getOrElse(c, -1))
    indices == indices.sorted && !indices.contains(-1)
  }


  "sort" should {
    "not add or remove anything" !
      forAll(genTest) { t => t.sorted.toSet == t.merged.toSet }

    "respect preferred order" !
      forAll(genTest) { t => isOrdered(t.sorted.filter(t.preferred.contains), t.preferred) }

    "respect alternate order" !
      forAll(genTest) { t =>
        val isPref = t.preferred.toSet

        def part(l: List[Char]): List[List[Char]] =
          (l:\List(List.empty[Char])) { case (c, res) =>
            if (isPref(c))
              res match {
                case Nil :: _ => res
                case _        => Nil :: res
              }
            else (c :: res.head) :: res.tail
          }

        part(t.sorted).forall { isOrdered(_,t.alternate) }
      }

    "place alternate chars close to preceding character in alternate order" !
      forAll(genTest) { t =>
        val isPref    = t.preferred.toSet
        val isMissing = (isPref ++ t.alternate.toSet) &~ t.merged.toSet

        def preds(l: List[Char]): Map[Char, List[Char]] = {
          val rev  = l.reverse
          rev.zip(rev.tails.drop(1).toIterable).toMap
        }

        val altPreds  = preds(t.alternate)
        val sortPreds = preds(t.sorted)

        t.sorted.forall { c =>
          isPref(c) || (sortPreds(c).headOption == altPreds(c).dropWhile(isMissing).headOption)
        }
      }
  }
}
