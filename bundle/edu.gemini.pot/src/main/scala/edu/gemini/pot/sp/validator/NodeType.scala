package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import scalaz._
import Scalaz._

/** A pair of shell type, component type. */
case class NodeType[N <: ISPNode : Manifest](ct: SPComponentType) {
  def mf = implicitly[Manifest[N]].runtimeClass
  def matches(n: ISPNode) = mf.isInstance(n) && n.getDataObject.getType == ct
  override def toString = mf.getSimpleName + "/" + ct

  def cardinalityOf(childType: NodeType[_ <: ISPNode]): NodeCardinality =
    Constraint.forType(this).map(_.cardinality(childType)).getOrElse(NodeCardinality.Zero)
}

object NodeType {
  def apply[A <: ISPNode : Manifest](n:A):NodeType[A] = NodeType[A](n.getDataObject.getType)

  def forNode(n: ISPNode): NodeType[_ <: ISPNode] =
    // This match expression peels off the proper manifest, so although it looks like
    // it doesn't do anything, it's important.
    n match {
      case n: ISPGroup              => NodeType(n)
      case n: ISPObsComponent       => NodeType(n)
      case n: ISPObservation        => NodeType(n)
      case n: ISPProgram            => NodeType(n)
      case n: ISPSeqComponent       => NodeType(n)
      case n: ISPTemplateFolder     => NodeType(n)
      case n: ISPTemplateGroup      => NodeType(n)
      case n: ISPTemplateParameters => NodeType(n)
      case n: ISPConflictFolder     => NodeType(n)
      case n: ISPObsQaLog           => NodeType(n)
      case n: ISPObsExecLog         => NodeType(n)
    }

  def forComponentType(ct: SPComponentType): Option[NodeType[_ <: ISPNode]] = {
    import edu.gemini.pot.sp.SPComponentType.{OBS_EXEC_LOG, OBS_QA_LOG, TEMPLATE_FOLDER, TEMPLATE_GROUP, TEMPLATE_PARAMETERS, QPT_CANOPUS, QPT_PWFS}
    import edu.gemini.pot.sp.SPComponentBroadType._

    def nt[A <: ISPNode : Manifest] = some(NodeType[A](ct))

    ct.broadType match {
      case AO          => nt[ISPObsComponent]
      case CONFLICT    => nt[ISPConflictFolder]
      case DATA        => none
      case ENGINEERING => nt[ISPObsComponent]
      case GROUP       => nt[ISPGroup]
      case INFO        => nt[ISPObsComponent]
      case INSTRUMENT  =>
        ct match { // Ugh
          case QPT_CANOPUS         => none
          case QPT_PWFS            => none
          case _                   => nt[ISPObsComponent]
        }
      case ITERATOR    => nt[ISPSeqComponent]
      case OBSERVATION => nt[ISPObservation]
      case OBSERVER    => nt[ISPSeqComponent]
      case OBSLOG      =>
        ct match {
          case OBS_EXEC_LOG        => nt[ISPObsExecLog]
          case OBS_QA_LOG          => nt[ISPObsQaLog]
          case _                   => none
        }
      case PLAN        => none
      case PROGRAM     => nt[ISPProgram]
      case SCHEDULING  => nt[ISPObsComponent]
      case TELESCOPE   => nt[ISPObsComponent]
      case TEMPLATE    =>
        ct match {
          case TEMPLATE_FOLDER     => nt[ISPTemplateFolder]
          case TEMPLATE_GROUP      => nt[ISPTemplateGroup]
          case TEMPLATE_PARAMETERS => nt[ISPTemplateParameters]
          case _                   => none
        }
      case UNKNOWN     => none
    }
  }
}
