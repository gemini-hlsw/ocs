package edu.gemini.model.p1.immutable.transform

import xml.{Node => XMLNode, _}

import scalaz._
import Scalaz._

object XMLConverter {
  /*
   * Contains the result of one conversion step
   */
  case class StepResult(change: List[String], node: Seq[XMLNode])

  object StepResult {
    // Placeholder for a zero node to be used in the monoid definition
    case object ZeroNode extends SpecialNode with Serializable {
      def label = ""

      def buildString(sb: StringBuilder) = sb.append("")
    }

    object ZeroStepResult extends StepResult(List.empty[String], ZeroNode)
    val zeroStepResult = StepResult(List.empty[String], ZeroNode)

    implicit val semigroupConversionResult:(StepResult, => StepResult) => StepResult = (a, b) => (a, b) match {
      case (x:StepResult,   ZeroStepResult) => x
      case (ZeroStepResult, x:StepResult)   => x
      case (x:StepResult,   y:StepResult)   => StepResult(x.change |+| y.change, y.node)
    }

    implicit val conversionResultMonoid = Monoid.instance[StepResult](semigroupConversionResult, zeroStepResult)

    def apply(change: String, node: Seq[XMLNode]):StepResult = StepResult(List(change), node)

    // Merge two step results as siblings
    val siblingGrouping = (a:StepResult, b:StepResult) => (a, b) match {
      case (x:StepResult,   ZeroStepResult) => x
      case (ZeroStepResult, x:StepResult)   => x
      case (x:StepResult,   y:StepResult)   => StepResult(x.change |+| y.change, List(x.node, y.node).flatten)
    }
    // Merge two step results as parent/children
    val parentGrouping = (p:StepResult, ch:StepResult) => StepResult(List(p.change, ch.change).flatten, Elem(p.node.head.prefix, p.node.head.label, p.node.head.attributes, p.node.head.scope, false, ch.node:_*))

    def flatten(s:List[Result]):Result =  {
      val vl = s.sequence[({type λ[α]=ValidationNel[String, α]})#λ, StepResult]
      vl.map(_.foldLeft(zeroStepResult)(_ |+| _))
    }

    def join(s:List[Result]):Result =  {
      val vl = s.sequence[({type λ[α]=ValidationNel[String, α]})#λ, StepResult]
      vl.map(_.foldLeft(zeroStepResult)(siblingGrouping))
    }

  }

  type Result = ValidationNel[String, StepResult]
  type TransformFunction = PartialFunction[XMLNode, Result]

  val fallbackTransform:TransformFunction = {
      case other        => StepResult(Nil, other).successNel
  }

  implicit val xmlNodeEqual: Equal[XMLNode] = Equal.equal(_ == _)

  def transform(n: XMLNode, transformers: TransformFunction*):Result = {
    def transformSingleNode(t: XMLConverter.TransformFunction)(node: XMLNode): Result = {
      if (node.doTransform) {
        if (node.child.isEmpty) {
          t.orElse(fallbackTransform).apply(node)
        } else {
          val processedChildren: ValidationNel[String, StepResult] = StepResult.join(node.child.toList.map(transformSingleNode(t)))
          val processedRoot: ValidationNel[String, StepResult] = t.orElse(fallbackTransform).apply(node)

          // These are the children of the root processed by root
          val rootChildren:ValidationNel[String, List[XMLNode]] = processedRoot.map((s:StepResult) => ~s.node.headOption.map(_.child.toList))

          // If the root changed its children it discards changes by the children transformers
          if (!rootChildren.map(_ === node.child.toList).exists(_ === true)) {
            processedRoot
          } else {
            (processedRoot |@| processedChildren)(StepResult.parentGrouping)
          }
        }
      } else {
        t.orElse(fallbackTransform).apply(node)
      }
    }

    transformers.foldLeft(StepResult(Nil, n).successNel[String])((r: ValidationNel[String, StepResult], transform:TransformFunction) => r.disjunction.flatMap { p =>
        val results = for {
          n <- p.node
        } yield (r |@| transformSingleNode(transform)(n))(_ |+| _)
        StepResult.flatten(results.toList).disjunction
      }.validation
    )
  }

}
