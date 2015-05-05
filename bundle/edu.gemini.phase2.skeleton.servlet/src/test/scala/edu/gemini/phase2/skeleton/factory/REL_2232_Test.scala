package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.GroupInitializer
import edu.gemini.phase2.template.factory.impl.nifs.{NifsAo, Nifs}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairNone
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.nifs.blueprint.{SpNifsBlueprintAo, SpNifsBlueprint}
import edu.gemini.spModel.target.SPTarget

import org.specs2.mutable.Specification

import scalaz.syntax.id._

class REL_2232_Test extends Specification {

  val NoteTitles = List("Phase II Requirements: General Information", "Phase II  \"BEFORE Submission\" Checklist")

  def incSpec(name: String, g: GroupInitializer[_], inc: Set[Int], excl: Set[Int]) = {
    s"Initialized $name" should {
      s"include obs ${inc.mkString("{",",","}")} in target group" in {
        g.targetGroup.filter(inc).toSet must_== inc
      }
      if (excl.nonEmpty) {
        s"exclude obs ${excl.mkString("{", ",", "}")} from target group" in {
          g.targetGroup.filter(excl) must beEmpty
        }
      }
      s"includes notes ${NoteTitles.mkString("{'", "','", "'}")}" in {
        g.notes must containAllOf(NoteTitles)
      }
    }
  }

  val bp     = new SpNifsBlueprint(NIFSParams.Disperser.DEFAULT)
  val bpAo   = new SpNifsBlueprintAo(SpAltairNone.instance, NIFSParams.Mask.CLEAR, NIFSParams.Disperser.DEFAULT)
  val bpAoOD = new SpNifsBlueprintAo(SpAltairNone.instance, NIFSParams.Mask.OD_1, NIFSParams.Disperser.DEFAULT)

  val nt  = new SPTarget
  val ht  = new SPTarget <| (_.getTarget.putMagnitude(new Magnitude(Band.H, 5.0)))
  def kt(m: Double)  = new SPTarget <| (_.getTarget.putMagnitude(new Magnitude(Band.K, m)))

  incSpec("NIFS template with no example target", Nifs(bp, None),         Set(2, 3, 4, 5), Set())
  incSpec("NIFS template with no magnitudes",     Nifs(bp, Some(nt)),     Set(2, 3, 4, 5), Set())
  incSpec("NIFS template with H-mag",             Nifs(bp, Some(ht)),     Set(2, 3, 4, 5), Set())
  incSpec("NIFS template with K-mag BT",          Nifs(bp, Some(kt(5))),  Set(2),          Set(3, 4, 5))
  incSpec("NIFS template with K-mag MT",          Nifs(bp, Some(kt(10))), Set(3),          Set(2, 4, 5))
  incSpec("NIFS template with K-mag FT",          Nifs(bp, Some(kt(15))), Set(4),          Set(2, 3, 5))
  incSpec("NIFS template with K-mag BAT",         Nifs(bp, Some(kt(21))), Set(5),          Set(2, 3, 4))

  incSpec("NIFS AO template with no example target", NifsAo(bpAo, None),         Set(2, 3, 4, 5), Set())
  incSpec("NIFS AO template with no magnitudes",     NifsAo(bpAo, Some(nt)),     Set(2, 3, 4, 5), Set())
  incSpec("NIFS AO template with H-mag",             NifsAo(bpAo, Some(ht)),     Set(2, 3, 4, 5), Set())
  incSpec("NIFS AO template with K-mag BT",          NifsAo(bpAo, Some(kt(5))),  Set(2),          Set(3, 4, 5))
  incSpec("NIFS AO template with K-mag MT",          NifsAo(bpAo, Some(kt(10))), Set(3),          Set(2, 4, 5))
  incSpec("NIFS AO template with K-mag FT",          NifsAo(bpAo, Some(kt(15))), Set(4),          Set(2, 3, 5))
  incSpec("NIFS AO template with K-mag BAT",         NifsAo(bpAo, Some(kt(21))), Set(5),          Set(2, 3, 4))

  incSpec("NIFS AO OD template with no example target", NifsAo(bpAoOD, None),         Set(11, 12), Set())
  incSpec("NIFS AO OD template with no magnitudes",     NifsAo(bpAoOD, Some(nt)),     Set(11, 12), Set())
  incSpec("NIFS AO OD template with H-mag",             NifsAo(bpAoOD, Some(ht)),     Set(11, 12), Set())
  incSpec("NIFS AO OD template with K-mag BT",          NifsAo(bpAoOD, Some(kt(5))),  Set(11),     Set(12))
  incSpec("NIFS AO OD template with K-mag MT",          NifsAo(bpAoOD, Some(kt(10))), Set(12),     Set(11))
  incSpec("NIFS AO OD template with K-mag FT",          NifsAo(bpAoOD, Some(kt(15))), Set(12),     Set(11))
  incSpec("NIFS AO OD template with K-mag BAT",         NifsAo(bpAoOD, Some(kt(21))), Set(12),     Set(11))

}
