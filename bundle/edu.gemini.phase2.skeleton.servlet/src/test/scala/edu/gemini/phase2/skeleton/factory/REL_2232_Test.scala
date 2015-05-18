package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.nifs.{ TargetBrightness, BAT, BT, FT, MT }
import edu.gemini.pot.sp.{ISPProgram, ISPTemplateGroup}
import edu.gemini.shared.skyobject.Magnitude.Band
import org.specs2.mutable.Specification
import edu.gemini.model.p1.immutable.{AltairNGS, Altair, NifsBlueprintAo, NifsBlueprintBase, NifsBlueprint}
import edu.gemini.model.p1.mutable.{NifsOccultingDisk, MagnitudeBand, NifsDisperser}
import org.specs2.specification.Example

object REL_2232_Test extends TemplateSpec("NIFS_BP.xml") with Specification {

  // All possible TargetBrightness buckets
  val buckets: List[Option[TargetBrightness]] =
    None :: List(BAT, FT, MT, BT).map(Some(_))

  // Return the TargetBrightness buckets for the targets in this template group. The returned
  // list should always have size = 1 but we will check that in a test below.
  def groupBuckets(g: ISPTemplateGroup): List[Option[TargetBrightness]] =
    p2targets(g)
      .map(p2mag(_, Band.K))
      .map(_.map(TargetBrightness(_)))

  // Return the TargetBrightness bucket for the first target in the given group. We establish by
  // testing groupBuckets above that this will return the one and only bucket for this group.
  def groupBucket(g: ISPTemplateGroup): Option[TargetBrightness] =
    groupBuckets(g).headOption.flatten

  // A map from TargetBrightness to group. We establish that this is a 1:1 mapping in the first
  // few tests below.
  def bucketMap(sp: ISPProgram): Map[Option[TargetBrightness], ISPTemplateGroup] =
    groups(sp)
      .map { g => groupBucket(g) -> g }
      .toMap

  // Our common tests for NIFS blueprint expansion
  def nifsTest(title: String, bp: NifsBlueprintBase)(more: ISPProgram => Example): Unit =
    expand(proposal(bp, (0.0 to 25.0 by 0.5).toList, MagnitudeBand.K)) { (p, sp) =>
      title >> {
        "All targets in a given group should be in the same target brightness bucket." in {
          groups(sp).map(groupBuckets).forall(_.distinct.size must_== 1)
        }
        "There should be a template group for each target brightness bucket." in {
          val found = groups(sp).map(groupBucket).toSet
          buckets.foreach(found)
        }
        "All groups should include all notes" in {
          List(
            "Phase II Requirements: General Information",
            "Phase II  \"BEFORE Submission\" Checklist"
          ).forall(n => groups(sp).forall(g => existsNote(g, n)))
        }
        more(sp)
      }
    }

  val nifs = NifsBlueprint(NifsDisperser.H)
  nifsTest("Non-AO NIFS Blueprint Expansion", nifs) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(2), Set(3, 4, 5))
    checkLibs("MT group",  map(Some(MT)),  Set(3), Set(2, 4, 5))
    checkLibs("FT group",  map(Some(FT)),  Set(4), Set(2, 3, 5))
    checkLibs("BAT group", map(Some(BAT)), Set(5), Set(2, 3, 4))
    checkLibs("Missing K-band group", map(None), Set(2, 3, 4, 5), Set())
  }

  val nifsAO = NifsBlueprintAo(AltairNGS(false), NifsOccultingDisk.CLEAR, NifsDisperser.H)
  nifsTest("NIFS AO without Occulting Disk Blueprint Expansion", nifsAO) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(2), Set(3, 4, 5))
    checkLibs("MT group",  map(Some(MT)),  Set(3), Set(2, 4, 5))
    checkLibs("FT group",  map(Some(FT)),  Set(4), Set(2, 3, 5))
    checkLibs("BAT group", map(Some(BAT)), Set(5), Set(2, 3, 4))
    checkLibs("Missing K-band group", map(None), Set(2, 3, 4, 5), Set())
  }

  val nifsAO_OD = NifsBlueprintAo(AltairNGS(false), NifsOccultingDisk.OD_2, NifsDisperser.H)
  nifsTest("NIFS AO with Occulting Disk Blueprint Expansion", nifsAO_OD) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(11), Set(12))
    checkLibs("MT group",  map(Some(MT)),  Set(12), Set(11))
    checkLibs("FT group",  map(Some(FT)),  Set(12), Set(11))
    checkLibs("BAT group", map(Some(BAT)), Set(12), Set(11))
    checkLibs("Missing K-band group", map(None), Set(11, 12), Set())
  }

}





