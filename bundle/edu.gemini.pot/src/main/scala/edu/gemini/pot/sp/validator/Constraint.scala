package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import scala.Left
import scala.Right


object Constraint {

  def forType(n:NodeType[_ <: ISPNode]):Option[Constraint] = forClass.get(n.mf)

  private val forClass:Map[Class[_], Constraint] = Map(
    classOf[ISPGroup] -> GroupConstraint.initial,
    classOf[ISPObservation] -> ObsConstraint.initial,
    classOf[ISPProgram] -> ProgramConstraint.initial,
    classOf[ISPSeqComponent] -> SeqConstraint.initial,
    classOf[ISPTemplateFolder] -> TemplateFolderConstraint.initial,
    classOf[ISPTemplateGroup] -> TemplateGroupConstraint.initial,
    classOf[ISPConflictFolder] -> ConflictConstraint.initial
  )
}


/**
 * A Constraint is a function that examines an ISPNode and either rejects it (returning a Violation) or
 * accepts it and returns a [possibly narrowed] Constraint intended to be applied to the node's next sibling.
 */
trait Constraint extends ((NodeType[_ <: ISPNode], Option[SPNodeKey]) => Either[Violation, Constraint]) {

  /** Override to specify unique narrow types that can occur only once among the node's children. */
  def uniqueNarrowTypes: Set[SPComponentType] = Set(SPComponentType.CONFLICT_FOLDER)

  /** Override to specify unique broad types that can occur only once among the node's children. */
  def uniqueBroadTypes: Set[SPComponentBroadType] = Set()

  /** Override to specify types that must occur among the node's children. */
  def requiredTypes: Set[SPComponentType] = Set()

  /** Construct a new instance of this constraint, with new Types. */
  def copy(newTypes: Types): Constraint

  /** Types for this constraint. */
  def types: Types

  /** True if constraint should be passed back up from children after visiting. Only true for SeqConstraint. */
  def returns:Boolean = false

  /** Return a new constraint for the specified child container. */
  def childConstraint(n: NodeType[_ <: ISPNode]): Option[Constraint] = Constraint.forType(n)

  /** Examine the specified node and return a new [possibly narrowed] Constraint if the node is allowed in this
    * context, otherwise return a Violation.
    */
  def apply(n: NodeType[_ <: ISPNode], key:Option[SPNodeKey]) = {

//    // TODO: get logging under control
//    println("Validating child %s/%s expecting one of\n\t%s".format(
//      n.mf.getSimpleName,
//      n.ct,
//      types.nodeTypes.mkString("\n\t")))

    if (types.matches(n)) {

      // Get the narrow and broad types
      val nt = n.ct
      val bt = nt.broadType

      // Check narrow first
      if (uniqueNarrowTypes.contains(nt)) {

        Right(copy(types - nt))

      } else {

        // Check the broad type
        if (uniqueBroadTypes.contains(bt))
          Right(copy(types - bt))
        else
          Right(this)

      }

    } else Left(CardinalityViolation(n, key, this))

  }

  def cardinality(n: NodeType[_ <: ISPNode]): NodeCardinality = {
    def mustBeUnique = uniqueBroadTypes.contains(n.ct.broadType) || uniqueNarrowTypes.contains(n.ct)
    if (types.matches(n)) {
      if (mustBeUnique) NodeCardinality.One else NodeCardinality.N
    } else NodeCardinality.Zero
  }
}