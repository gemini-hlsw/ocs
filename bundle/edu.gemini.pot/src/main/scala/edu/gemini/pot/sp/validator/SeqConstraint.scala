package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.SPComponentBroadType._
import scala.Some

object SeqConstraint {

  // These types can occur in any sequence
  val genericTypes: Types = Types.initial
    .addBroad[ISPSeqComponent](OBSERVER)
    .addNarrow[ISPSeqComponent](ITERATOR_CALUNIT, ITERATOR_REPEAT)

  def initialTypesForInstrument(ct: SPComponentType): Types = ct match {
    case INSTRUMENT_GHOST => genericTypes - OBSERVER_DARK - OBSERVER_GEMFLAT
    case _                => genericTypes - OBSERVER_GHOST_DARK - OBSERVER_GHOST_GEMFLAT
  }

  // Almost all instruments have their own iterator type
  def iteratorFor(ct:SPComponentType):Option[SPComponentType] = Some(ct) collect {
    case INSTRUMENT_ACQCAM     => ITERATOR_ACQCAM
    case INSTRUMENT_BHROS      => ITERATOR_BHROS
    case INSTRUMENT_FLAMINGOS2 => ITERATOR_FLAMINGOS2
    case INSTRUMENT_GHOST      => ITERATOR_GHOST
    case INSTRUMENT_GMOS       => ITERATOR_GMOS
    case INSTRUMENT_GMOSSOUTH  => ITERATOR_GMOSSOUTH
    case INSTRUMENT_GNIRS      => ITERATOR_GNIRS
    case INSTRUMENT_GPI        => ITERATOR_GPI
    case INSTRUMENT_GSAOI      => ITERATOR_GSAOI
    case INSTRUMENT_MICHELLE   => ITERATOR_MICHELLE
    case INSTRUMENT_NICI       => ITERATOR_NICI
    case INSTRUMENT_NIFS       => ITERATOR_NIFS
    case INSTRUMENT_NIRI       => ITERATOR_NIRI
    case INSTRUMENT_PHOENIX    => ITERATOR_PHOENIX
    case INSTRUMENT_TRECS      => ITERATOR_TRECS
  }

  // Some instruments have special offset iterators
  def offsetIteratorFor(ct:SPComponentType) = ct match {
    case INSTRUMENT_GPI  => Seq(ITERATOR_OFFSET)
    case INSTRUMENT_NICI => Seq(ITERATOR_NICIOFFSET)
    case _               => Seq(ITERATOR_OFFSET)
  }

  val initial = forInstrument(None)

  def forInstrument(ct:SPComponentType):SeqConstraint = forInstrument(Some(ct))

  // If the instrument has been specified, we can narrow the constraint on the sequence.
  // Otherwise anything is allowed (initially).
  def forInstrument(ct:Option[SPComponentType]):SeqConstraint = new SeqConstraint(ct.map { ct =>
    initialTypesForInstrument(ct).addNarrow[ISPSeqComponent](offsetIteratorFor(ct) ++ iteratorFor(ct).toList : _*)
  }.getOrElse {
    genericTypes.addBroad[ISPSeqComponent](ITERATOR)
  })

}

/** A constraint for the children of a sequence node. */
case class SeqConstraint private(val types: Types) extends Constraint {
  import SeqConstraint._

  def copy(ts: Types) = new SeqConstraint(ts)

  // All children are also sequence nodes, and they're constrained in the same way
  // Update: EXCEPT observer nodes, which have no children!
  override def childConstraint(n: NodeType[_ <: ISPNode]) =
    if (n.ct.broadType == OBSERVER) None
    else if (n.ct == CONFLICT_FOLDER) Some(ConflictConstraint)
    else Some(this)

  // When we descend into a child collection and return, we pass the constraint to the next child.
  // This is distinct from the other node types, where subtrees can't constrain one another.
  override def returns = true

  override def apply(n: NodeType[_ <: ISPNode], key:Option[SPNodeKey]) =
  for {
    c <- super.apply(n, key).right
  } yield {

    n.ct match {

      // An instrument-specific iterator restricts any future iterators
      case ITERATOR_ACQCAM     => forInstrument(INSTRUMENT_ACQCAM)
      case ITERATOR_BHROS      => forInstrument(INSTRUMENT_BHROS)
      case ITERATOR_FLAMINGOS2 => forInstrument(INSTRUMENT_FLAMINGOS2)
      case ITERATOR_GHOST      => forInstrument(INSTRUMENT_GHOST)
      case ITERATOR_GMOS       => forInstrument(INSTRUMENT_GMOS)
      case ITERATOR_GMOSSOUTH  => forInstrument(INSTRUMENT_GMOSSOUTH)
      case ITERATOR_GNIRS      => forInstrument(INSTRUMENT_GNIRS)
      case ITERATOR_GPI        => forInstrument(INSTRUMENT_GPI)
      case ITERATOR_GPIOFFSET  => forInstrument(INSTRUMENT_GPI) // NOTE
      case ITERATOR_GSAOI      => forInstrument(INSTRUMENT_GSAOI)
      case ITERATOR_MICHELLE   => forInstrument(INSTRUMENT_MICHELLE)
      case ITERATOR_NICI       => forInstrument(INSTRUMENT_NICI)
      case ITERATOR_NICIOFFSET => forInstrument(INSTRUMENT_NICI) // NOTE
      case ITERATOR_NIFS       => forInstrument(INSTRUMENT_NIFS)
      case ITERATOR_NIRI       => forInstrument(INSTRUMENT_NIRI)
      case ITERATOR_PHOENIX    => forInstrument(INSTRUMENT_PHOENIX)
      case ITERATOR_TRECS      => forInstrument(INSTRUMENT_TRECS)

      // Otherwise do nothing
      case _ => c

    }
  }

}
