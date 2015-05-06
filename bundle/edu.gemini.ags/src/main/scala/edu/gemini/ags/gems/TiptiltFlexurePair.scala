package edu.gemini.ags.gems

import edu.gemini.spModel.gems.GemsGuideStarType
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

case class TiptiltFlexurePair(tiptiltResults: GemsCatalogSearchResults, flexureResults: GemsCatalogSearchResults)

/**
 * Groups a pair of GemsCatalogSearchResults to be used for tiptilt and flexure stars.
 * The two results must be from different guide probe groups (Canopus, GSAOI, etc).
 */
object TiptiltFlexurePair {
  private case class TipTiltFlexure(tt: Option[GemsCatalogSearchResults], flex: Option[GemsCatalogSearchResults])

  def pairs(results: List[GemsCatalogSearchResults]): List[TiptiltFlexurePair] = {
    val pairs = (TipTiltFlexure(None, None), TipTiltFlexure(None, None))
    // Go Over the results and assign them to buckets, it will keep the last result for each subgroup
    val resultPair = results.foldLeft(pairs) { (p, v) =>
      v.criterion.key match {
        case k if k.starType == GemsGuideStarType.tiptilt && k.group.getKey == "CWFS"                                    => (p._1.copy(tt = v.some), p._2)
        case k if k.starType == GemsGuideStarType.tiptilt && k.group.getKey == "ODGW"                                    => (p._1, p._2.copy(tt = v.some))
        case k if k.starType == GemsGuideStarType.flexure && (k.group.getKey == "ODGW" || k.group.getKey == "FII OIWFS") => (p._1.copy(flex = v.some), p._2)
        case k if k.starType == GemsGuideStarType.flexure && k.group.getKey == "CWFS"                                    => (p._1, p._2.copy(flex = v.some))
      }
    }
    // If they have valid results convert them to pairs
    List(resultPair._1, resultPair._2).collect {
      case TipTiltFlexure(Some(tt), Some(flex)) => TiptiltFlexurePair(tt, flex)
    }
  }
}
