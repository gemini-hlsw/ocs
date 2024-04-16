package edu.gemini.phase2.template.factory.impl

import edu.gemini.phase2.core.model.{GroupShell, ObsComponentShell, ObservationShell}
import edu.gemini.phase2.template.factory.api.{BlueprintExpansion, TemplateFactory}
import edu.gemini.phase2.template.factory.impl.phoenix.Phoenix
import edu.gemini.spModel.gemini.gmos.blueprint._
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.template.{Phase1Group, SpBlueprint}
import flamingos2.{Flamingos2Imaging, Flamingos2Longslit, Flamingos2Mos}
import gmos._
import gnirs.{GnirsImaging, GnirsSpectroscopy}
import gsaoi.Gsaoi
import igrins2.Igrins2
import michelle.{MichelleImaging, MichelleSpectroscopy}
import nici.{NiciCoronographic, NiciStandard}
import nifs.{Nifs, NifsAo}
import niri.Niri

import scala.collection.JavaConverters._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.gemini.gmos.GmosCommonType.UseNS
import edu.gemini.spModel.gemini.flamingos2.blueprint.{SpFlamingos2BlueprintImaging, SpFlamingos2BlueprintLongslit, SpFlamingos2BlueprintMos}
import edu.gemini.phase2.template.factory.impl.ghost.Ghost
import edu.gemini.spModel.gemini.gnirs.blueprint.{SpGnirsBlueprintImaging, SpGnirsBlueprintSpectroscopy}
import edu.gemini.spModel.gemini.michelle.blueprint.{SpMichelleBlueprintImaging, SpMichelleBlueprintSpectroscopy}
import edu.gemini.spModel.gemini.nici.blueprint.{SpNiciBlueprintCoronagraphic, SpNiciBlueprintStandard}
import edu.gemini.spModel.gemini.nifs.blueprint.{SpNifsBlueprint, SpNifsBlueprintAo}
import edu.gemini.spModel.gemini.niri.blueprint.SpNiriBlueprint
import edu.gemini.spModel.obs.ObsPhase2Status
import edu.gemini.pot.sp._
import edu.gemini.spModel.obscomp.SPGroup
import edu.gemini.spModel.gemini.gsaoi.blueprint.SpGsaoiBlueprint
import edu.gemini.spModel.gemini.texes.blueprint.SpTexesBlueprint
import texes.Texes
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint
import visitor.Visitor
import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint
import edu.gemini.phase2.template.factory.impl.gpi.Gpi
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.phase2.template.factory.impl.graces.Graces
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.phase2.template.factory.impl.gnirs.GnirsSpectroscopyIfu
import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprint
import edu.gemini.spModel.gemini.igrins2.blueprint.SpIgrins2Blueprint

case class TemplateFactoryImpl(db: TemplateDb) extends TemplateFactory {

  type TargetId = String

  def expand(blueprint: SpBlueprint, pig: Phase1Group, preserveLibraryIds: Boolean, pid: SPProgramID): Either[String, BlueprintExpansion] = {
    // template groups are editable and all arguments can be removed so there
    // may not be an example target
    def sampleTarget: Option[SPTarget] =
      if (pig.argsList.size() == 0) None
      else Some(pig.argsList.get(0).getTarget)

    for {
      ini <- initializer(blueprint, sampleTarget).right
      grp <- ini.initialize(db, pid).right
    } yield convert(blueprint, pig, grp, ini, preserveLibraryIds)
  }

  // Provide a sample target, used in some cases to get the magnitude. It is assumed that the groups are partitioned
  // such that any target is a good example (we use the first one). This happens in TemplateFolderFactory, sadly.
  private def initializer(blue: SpBlueprint, sampleTarget: Option[SPTarget]): Either[String, GroupInitializer[_]] =
    blue match {

      // FLAMINGOS-2
      case b: SpFlamingos2BlueprintImaging => Right(Flamingos2Imaging(b))
      case b: SpFlamingos2BlueprintLongslit => Right(Flamingos2Longslit(b, sampleTarget))
      case b: SpFlamingos2BlueprintMos => Right(Flamingos2Mos(b))

      // GHOST
      case b: SpGhostBlueprint => Right(Ghost(b))

      // GMOS-N
      case b: SpGmosNBlueprintIfu => Right(GmosNIfu(b))
      case b: SpGmosNBlueprintImaging => Right(GmosNImaging(b))
      case b: SpGmosNBlueprintLongslit => Right(GmosNLongslit(b))
      case b: SpGmosNBlueprintLongslitNs => Right(GmosNLongslitNs(b))
      case b: SpGmosNBlueprintMos => Right(if (b.nodAndShuffle == UseNS.TRUE) GmosNMosNs(b) else GmosNMos(b))

      // GMOS-S
      case b: SpGmosSBlueprintIfu => Right(GmosSIfu(b))
      case b: SpGmosSBlueprintIfuNs => Right(GmosSIfuNs(b))
      case b: SpGmosSBlueprintImaging => Right(GmosSImaging(b))
      case b: SpGmosSBlueprintLongslit => Right(GmosSLongslit(b))
      case b: SpGmosSBlueprintLongslitNs => Right(GmosSLongslitNs(b))
      case b: SpGmosSBlueprintMos => Right(if (b.nodAndShuffle == UseNS.TRUE) GmosSMosNs(b) else GmosSMos(b))

      // GNIRS
      case b: SpGnirsBlueprintImaging => Right(GnirsImaging(b))
      case b: SpGnirsBlueprintSpectroscopy =>
        if (b.fpu.isIfu) Right(GnirsSpectroscopyIfu(b, sampleTarget))
        else Right(GnirsSpectroscopy(b, sampleTarget))

      // GPI
      case b: SpGpiBlueprint => Right(Gpi(b))

      // GSAOI
      case b: SpGsaoiBlueprint => Right(Gsaoi(b))

      // Igrins2
      case b: SpIgrins2Blueprint => Right(Igrins2(b))

      // MICHELLE
      case b: SpMichelleBlueprintImaging => Right(MichelleImaging(b))
      case b: SpMichelleBlueprintSpectroscopy => Right(MichelleSpectroscopy(b))

      // NICI
      case b: SpNiciBlueprintCoronagraphic => Right(NiciCoronographic(b))
      case b: SpNiciBlueprintStandard => Right(NiciStandard(b))

      // NIFS
      case b: SpNifsBlueprintAo => Right(NifsAo(b, sampleTarget))
      case b: SpNifsBlueprint => Right(Nifs(b, sampleTarget))

      // NIRI
      case b: SpNiriBlueprint => Right(Niri(b))

      // TEXES
      case b: SpTexesBlueprint => Right(Texes(b))

      // GRACES
      case b: SpGracesBlueprint => Right(Graces(b, sampleTarget))

      // PHOENIX
      case b: SpPhoenixBlueprint => Right(Phoenix(b))

      // VISITOR
      case b: SpVisitorBlueprint => Right(Visitor(b))

      case _ => Left("Could not find template group factory for %s".format(blue))
    }


  private def convert(blue: SpBlueprint, pig: Phase1Group, grp: ISPGroup, ini:GroupInitializer[_], preserveLibraryIds: Boolean): BlueprintExpansion = {

    // Template obs
    val tids:Seq[String] = ini.targetGroup.map(_.toString)

    // Rename the group to match the blueprint name.
    grp.update(_.setTitle(blue.toString))

    // Sort template obs by library number
    grp.setObservations(grp.getObservations.asScala.sortBy(_.libraryId.map(_.toInt).get).asJava)

    // Split template from cals
    val (templates, cals) = grp.getObservations.asScala.partition(_.libraryId.exists(tids.contains))

    // Remove the library ids
    if (!preserveLibraryIds) {
      (templates ++ cals) foreach { o =>
        o.update(_.setLibraryId(null))
        o.update(_.setPhase2Status(ObsPhase2Status.PI_TO_COMPLETE))
        o.update(_.setExecStatusOverride(edu.gemini.shared.util.immutable.None.instance()))
      }
    }

    // Remove the baseline calibrations from the template group.
    grp.setObservations(templates.asJava)

    // Add all notes to target group for now
    val p = db.progMap(ini.program)
    ini.notes.flatMap(findNote(p)(_).toList).foreach { n =>
      val n0 = db.odb.getFactory.createObsComponentCopy(grp.getProgram, n, false)
      grp.addObsComponent(n0)
    }

    // Convert to shells.
    val templateGroup = new GroupShell(grp).toTemplateGroupShell(pig)

    // Create the baseline calibration group
    val baselineDataObj = new SPGroup("Baseline: " + blue.toString)
    baselineDataObj.setGroupType(SPGroup.GroupType.TYPE_FOLDER)
    val emptyComps  = List.empty[ObsComponentShell].asJava
    val baselineObs = cals.map(c => new ObservationShell(c)).asJava
    val baselineGroup = new GroupShell(baselineDataObj, emptyComps, baselineObs)

    BlueprintExpansion(templateGroup, baselineGroup)

  }

  def findNote(n:ISPNode)(title:String): Option[ISPObsComponent] =
    n.findDescendant {
      case c: ISPObsComponent => isMatchingNote(c, title)
      case _                  => false
    }.map(_.asInstanceOf[ISPObsComponent])

  def isMatchingNote(n:ISPObsComponent, title:String): Boolean =
    n.getType match {
      case SPComponentType.INFO_NOTE | SPComponentType.INFO_SCHEDNOTE =>
        n.getDataObject.getTitle == title
      case _                                                          =>
        false
    }

}
