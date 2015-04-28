package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.phase2.template.factory.impl.nifs.{NifsAo, NifsBase, Nifs}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.altair.blueprint.SpAltairNone
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.nifs.blueprint.{SpNifsBlueprintAo, SpNifsBlueprint}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.rich.pot.sp._

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz.syntax.id._

class REL_2232_Test extends Specification {

  lazy val tdb = TemplateDb.load(
    java.util.Collections.emptySet[java.security.Principal],
    _.toString.contains("NIFS_BP.xml")).right.get

  def libraryIds(t: NifsBase[_]): Set[Int] =
    t.initialize(tdb).right.get.getAllObservations.asScala.flatMap(_.libraryId).map(_.toInt).toSet

  def incSpec(name: String, t: NifsBase[_], inc: Set[Int], excl: Set[Int], note: Boolean) = {
    lazy val ids = libraryIds(t)
    name should {
      if (excl.nonEmpty) {
        s"exclude ${excl.mkString("{", ",", "}")}" in {
          (ids & excl) must beEmpty
        }
      }
      s"include ${inc.mkString("{",",","}")}" in {
        (ids & inc) must_== inc
      }
      if (note) {
        "includes note" in {
          false
        }
      } else {
        "excludes note" in {
          false
        }
      }
    }
  }

  val bp     = new SpNifsBlueprint(NIFSParams.Disperser.DEFAULT)
  val bpAo   = new SpNifsBlueprintAo(SpAltairNone.instance, NIFSParams.Mask.CLEAR, NIFSParams.Disperser.DEFAULT)
  val bpAoOD = new SpNifsBlueprintAo(SpAltairNone.instance, NIFSParams.Mask.OD_1, NIFSParams.Disperser.DEFAULT)

  val ht  = new SPTarget <| (_.getTarget.putMagnitude(new Magnitude(Band.H, 5.0))) // bright
  val nht = new SPTarget

  incSpec("NIFS template with no example target",       Nifs(bp, None), Set(3, 4, 5), Set(), true)
  incSpec("NIFS template with no H-mag",                Nifs(bp, Some(nht)), Set(3, 4, 5), Set(), true)
  incSpec("NIFS template with h-mag",                   Nifs(bp, Some(ht)), Set(3), Set(4, 5), false)

  incSpec("NIFS AO template with no example target",    NifsAo(bpAo, None), Set(3, 4, 5), Set(), true)
  incSpec("NIFS AO template with no H-mag",             NifsAo(bpAo, Some(nht)), Set(3, 4, 5), Set(), true)
  incSpec("NIFS AO template with h-mag",                NifsAo(bpAo, Some(ht)), Set(3), Set(4, 5), true)

  incSpec("NIFS AO OD template with no example target", NifsAo(bpAoOD, None), Set(11, 12), Set(), true)
  incSpec("NIFS AO OD template with no H-mag",          NifsAo(bpAoOD, Some(nht)), Set(11, 12), Set(), true)
  incSpec("NIFS AO OD template with h-mag",             NifsAo(bpAoOD, Some(ht)), Set(11), Set(12), true)


}
