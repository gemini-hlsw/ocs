package edu.gemini.sp.vcs2

import scala.annotation.tailrec

/** A heuristic for sorting an unordered merged list derived from two sorted
  * source lists. */
private[vcs2] object SortHeuristic {

  /** Produces a list sort function from a pair of "guideline" sorted lists.
    * Given a preferred key order (which may not contain all the elements of the
    * eventual input list), and an alternate key order (which also may not
    * contain all the elements of the eventual input list) produce a sort
    * function that takes an input list and sorts it trying to respect the
    * preferred and alternate orderings.
    *
    * This is loosely worded because there is no clear best result in all cases.
    * The intuition is that `preferred` and `alternate` are usually roughly the
    * same, give or take an element or two with some possible rearrangements.
    * The input list for the returned sort function represents a merge of the
    * preferred and alternate lists.  Its elements map to keys that should be in
    * one or the other (or both) of `preferred` and `alternate`.
    *
    * The laws, if you will, are that
    *
    * (a) all input elements mapping to keys in `preferred` must appear in the
    *     same relative order as they appear in `preferred`
    *
    * (b) all input elements mapping to keys that appear exclusively in
    *     `alternative` (i.e., not in `preferred`) should be placed behind the
    *     closest predecessor in `alternative` that is also in `input`
    *
    * @param preferred keys in preferred order
    * @param alternate keys in alternate order
    *
    * @param kf key function, extracts a unique key from an A
    *
    * @tparam A element type of input list
    * @tparam K type of key in `preferred` and `alternate`
    *
    * @return a "reasonably" sorted list according to the description above
    */
  def sort[A,K](input: List[A], preferred: List[K], alternate: List[K])(kf: A => K): List[A] = {
    val pMap = preferred.zipWithIndex.toMap

    // First sort (reversed) all the elements that are in `preferred`.
    val (found, missing) = input.partition(a => pMap.contains(kf(a)))
    val revSort          = found.sortBy(a => -pMap(kf(a)))

    // Get map of K -> List[K] of predecessors in the alternative list.
    // For example, given 'a', 'b', 'c':
    //    'a' -> []
    //    'b' -> ['a']
    //    'c' -> ['b', 'a']
    lazy val altRev      = alternate.reverse
    lazy val altPreds    = altRev.zip(altRev.tails.drop(1).toIterable).toMap

    @tailrec
    def insert(as: List[A], a: A, predecessors: List[K]): List[A] =
      predecessors match {
        case Nil     => as :+ a
        case p :: ps =>
          val (suffix, prefix) = as.span(kf(_) != p)
          if (prefix.nonEmpty) suffix ++ (a :: prefix)
          else insert(as, a, ps) // go fish
      }

    // Fold all the missing elements into the reverse sorted list, then finally
    // reverse the result to get the desired order.
    (revSort/:missing) { (as,a) => insert(as, a, altPreds.getOrElse(kf(a), Nil)) }.reverse
  }
}