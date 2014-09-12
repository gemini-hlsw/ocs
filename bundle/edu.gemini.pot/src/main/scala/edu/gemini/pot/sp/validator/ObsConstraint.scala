package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.SPComponentBroadType._
import edu.gemini.pot.sp._

object ObsConstraint {
  val initial = new ObsConstraint(Types.initial
    .addNarrow[ISPSeqComponent](ITERATOR_BASE)
    .addNarrow[ISPObsQaLog](OBS_QA_LOG)
    .addNarrow[ISPObsExecLog](OBS_EXEC_LOG)
    .addBroad[ISPObsComponent](INSTRUMENT, ENGINEERING, AO, SCHEDULING, TELESCOPE, INFO),
    None)
}

/**
 * A Constraint for the children of an ISPObservation. This constraint has a progressively narrowing set of allowed types,
 * and accumulates a single value (for the instrument, if any).
 */
case class ObsConstraint private(types: Types, inst: Option[SPComponentType]) extends Constraint {

  // These broad types can occur only once, so remove them from `ts` when we encounter them.
  override val uniqueNarrowTypes = super.uniqueNarrowTypes ++ Set(ITERATOR_BASE, OBS_QA_LOG, OBS_EXEC_LOG)
  override val uniqueBroadTypes  = Set(INSTRUMENT, ENGINEERING, AO, SCHEDULING, TELESCOPE)
  override val requiredTypes     = Set(OBS_EXEC_LOG, OBS_QA_LOG, ITERATOR_BASE)

  def copy(ts: Types) = new ObsConstraint(ts, inst)

  // We need to override in order to accumulate the instrument and further constrain the set of allowed types based
  // on AO + Instrument compatibility.
  override def apply(n: NodeType[_ <: ISPNode], key:Option[SPNodeKey]) =
    for {
      c <- super.apply(n, key).right
    } yield {
      val t = n.ct
      t.broadType match {

        // If it's an engineering node, remember the instrument and constrain AO+Inst
        case ENGINEERING =>
          val inst = instrumentFor(t)
          val ts0 = constrainInst(t, c.types)
          val ts1 = constrainAO(inst, ts0)
          new ObsConstraint(ts1, Some(inst))

        // If it's an instrument, remember the instrument and constrain AO+Eng
        case INSTRUMENT =>
          val ts0 = constrainAO(t, c.types)
          val ts1 = constrainEng(t, ts0)
          new ObsConstraint(ts1, Some(t))

        // If it's AO, constrain the instrument
        case AO => copy(constrainInstrument(t, c.types))

        case _ => c
      }
    }

  // Given an engineering component, get the instrument
  def instrumentFor(t:SPComponentType) = {
    require(t.broadType == ENGINEERING, "Not an engineering instrument: " + t)
    t match {
      case ENG_ENGNIFS  => INSTRUMENT_NIFS
      case ENG_ENGTRECS => INSTRUMENT_TRECS
    }
  }

  // Given an instrument, constrain the engineering instrument
  def constrainEng(t:SPComponentType, ts:Types) = {
    require(t.broadType == INSTRUMENT, "Not an instrument type: " + t)
    t match {
      case INSTRUMENT_NIFS  => ts.retainOnly(ENGINEERING, ENG_ENGNIFS)
      case INSTRUMENT_TRECS => ts.retainOnly(ENGINEERING, ENG_ENGTRECS)
      case _ => ts - ENGINEERING
    }
  }

  // Given an engineering instrument, constrain the science instrument
  def constrainInst(t:SPComponentType, ts:Types) = {
    require(t.broadType == ENGINEERING, "Not an engineering instrument: " + t)
    t match {
      case ENG_ENGNIFS  => ts.retainOnly(INSTRUMENT, INSTRUMENT_NIFS)
      case ENG_ENGTRECS => ts.retainOnly(INSTRUMENT, INSTRUMENT_TRECS)
    }
  }

  // Give a component type and a set of types, return a new set of types constrained for the compatible AO options.
  // Note that it only makes sense for `t` to be an instrument type.
  def constrainAO(t: SPComponentType, ts: Types) = {
    require(t.broadType == INSTRUMENT, "Not an instrument type: " + t)
    t match {

      // Acq Cam can be with GeMS or Altair
      case INSTRUMENT_ACQCAM => ts.retainOnly(AO, AO_GEMS, AO_ALTAIR)

      // These can only use GEMS
      case INSTRUMENT_FLAMINGOS2
         | INSTRUMENT_GMOSSOUTH
         | INSTRUMENT_GSAOI  => ts.retainOnly(AO, AO_GEMS)

      // These can only use Altair
      case INSTRUMENT_GNIRS
         | INSTRUMENT_NIFS
         | INSTRUMENT_NIRI
         | INSTRUMENT_GMOS => ts.retainOnly(AO, AO_ALTAIR)

      // Otherwise AO is not allowed
      case _ => ts - AO

    }
  }

  // Give a component type and a set of types, return a new set of types constrained for the compatible instruments.
  // Note that it only makes sense for `t` to be an AO type.
  def constrainInstrument(t: SPComponentType, ts: Types) = {
    require(t.broadType == AO, "Not an AO type: " + t)
    t match {
      case AO_GEMS   => ts.retainOnly(INSTRUMENT, INSTRUMENT_ACQCAM, INSTRUMENT_FLAMINGOS2, INSTRUMENT_GMOSSOUTH, INSTRUMENT_GSAOI)
      case AO_ALTAIR => ts.retainOnly(INSTRUMENT, INSTRUMENT_ACQCAM, INSTRUMENT_GNIRS, INSTRUMENT_NIFS, INSTRUMENT_NIRI, INSTRUMENT_GMOS)
    }
  }

  // The only container in an observation is the sequence, which we initialize with our instrument, if any
  override def childConstraint(n: NodeType[_ <: ISPNode]):Option[Constraint] =
         if (n.mf == classOf[ISPSeqComponent]) Some(SeqConstraint.forInstrument(inst))
    else super.childConstraint(n)

}

