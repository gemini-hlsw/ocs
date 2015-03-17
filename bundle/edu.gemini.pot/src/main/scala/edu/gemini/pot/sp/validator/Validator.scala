package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.{SPNodeKey, ISPNode, ISPProgram, ISPContainerNode}
import java.awt.{AWTEvent, EventQueue}

import scala.util.Try

object EventCache {

  private var previousEvent: Option[AWTEvent] = None
  private val cache: collection.mutable.Map[(ISPContainerNode, Option[SPNodeKey]), TypeTree] = collection.mutable.Map()

  def tree(prog: ISPContainerNode, contextKey: Option[SPNodeKey])(a: => TypeTree): TypeTree =
    Option(Try(EventQueue.getCurrentEvent).toOption.orNull) match {
      case None => a
      case o =>
        if (o != previousEvent) {
          previousEvent = o
          cache.clear()
        }
        cache.getOrElseUpdate((prog, contextKey), a)
    }
}

object Validator {
//  def canAdd(prog: ISPProgram, newNode: ISPNode, parent: ISPNode): Boolean =
//    canAdd(prog, Array(newNode), parent)
//
//  def canAdd(prog: ISPProgram, newNodes: Array[ISPNode], parent: ISPNode): Boolean = {
//    def deepCheck: Boolean = {
//      val tt = EventCache.tree(prog)(TypeTree(prog))
//      val ins = (tt/:newNodes) { (typeTree, newNode) =>
//        typeTree.insert(parent.getNodeKey, TypeTree(newNode).withoutKeys)
//      }
//      validate(ins).isRight
//    }
//
//    val parentType = NodeType.forNode(parent)
//    val shallowCheck = newNodes.forall { nn =>
//      parentType.cardinalityOf(NodeType.forNode(nn)).toInt > 0
//    }
//
//    shallowCheck && deepCheck
//  }

  // The 'context' node, if provided, is used to trim the type tree down to the
  // minimum required for an accurate validation.  In particular, irrelevant
  // observations and groups are removed.  Irrelevant in the sense that they do
  // not contain the context node and are not in the path from the root down to
  // the context node.

  def canAdd(prog: ISPProgram, newNodes: Array[ISPNode], parent: ISPNode, context: Option[ISPNode]): Boolean = {
    def deepCheck: Boolean = {
      val nodeKey = context.flatMap(c => Option(c.getNodeKey))
      val tt = EventCache.tree(prog, nodeKey) {
        nodeKey.flatMap(k => TypeTree.validationTree(prog, k)).getOrElse(TypeTree(prog))
      }
      val ins = (tt/:newNodes) { (typeTree, newNode) =>
        typeTree.insert(parent.getNodeKey, TypeTree(newNode).withoutKeys)
      }
      validate(ins).isRight
    }

    val parentType = NodeType.forNode(parent)
    val shallowCheck = newNodes.forall { nn =>
      parentType.cardinalityOf(NodeType.forNode(nn)).toInt > 0
    }

    shallowCheck && deepCheck
  }

  /** Validate the given type tree, which must be of a container node type. */
  def validate(tree: TypeTree): Either[Violation, Constraint] =
    for {

// RCN: this is very expensive and is just a sanity check, so we're turning it off for now
//    _ <- validateKeysAreUnique(tree).right

      c <- Constraint.forType(tree.node).toRight(CardinalityViolation(tree.node, tree.key, null)).right // TODO: FIX
      c <- validate(c, tree).right
    } yield c

  /** Validate the given container node. */
  def validate(n: ISPContainerNode): Either[Violation, Constraint] =
    validate(EventCache.tree(n, None)(TypeTree(n)))

//  /** Check for duplicate keys. We can do this in a single pass. */
//  private def validateKeysAreUnique(tree: TypeTree): Either[Violation, Set[SPNodeKey]] = {
//
//    @tailrec // breadth-first accumulation of keys
//    def accum(ks: Set[SPNodeKey], q: Queue[TypeTree]): Either[Violation, Set[SPNodeKey]] =
//      q.headOption match {
//        case None => Right(ks)
//        case Some(TypeTree(_, None, children)) => accum(ks, q.tail ++ children)
//        case Some(t@TypeTree(_, Some(k), children)) =>
//          if (ks.contains(k))
//            Left(DuplicateKeyViolation(k))
//          else
//            accum(ks + k, q.tail ++ children)
//      }
//
//    accum(Set(), Queue(tree))
//
//  }

  // TODO: bind to the event
  private var previousEvent: Option[AWTEvent] = None
  private val cache = collection.mutable.Map[(Constraint, TypeTree), Either[Violation, Constraint]]()

  private def validate(c: Constraint, tree: TypeTree): Either[Violation, Constraint] = {
    lazy val a = validate1(c, tree)
    Option(Try(EventQueue.getCurrentEvent).toOption.orNull) match {
      case None => a
      case o =>
        if (o != previousEvent) {
          previousEvent = o
          cache.clear()
        }
        cache.getOrElseUpdate((c, tree), a)
    }
  }


    /** Validate the given tree with the specified constraint. */
  private def validate1(c: Constraint, tree: TypeTree): Either[Violation, Constraint] = {

    // N.B. these differ from standard folds in subtle ways. Use caution if you want to
    // replace with something from scalaz, for example.

    // Flat validation across a single ply
    def flat(c: Constraint, ns: List[TypeTree]): Either[Violation, Constraint] =
      ((Right(c): Either[Violation, Constraint]) /: ns) {
        case (v@Left(_), _) => v
        case (Right(c0), n) => c0(n.node, n.key)
      }

    // Deep validation
    def deep(c: Constraint, ns: List[TypeTree]): Either[Violation, Constraint] =
      ns match {
        case n :: ns0 if n.children.nonEmpty =>
          c.childConstraint(n.node) match {
            case Some(ConflictConstraint) => deep(c, ns0)
            case Some(cc) => validate(cc, n) match {
              case v@Left(_) => v
              case Right(c0) => deep(if (c.returns) c0 else c, ns0)
            }
            // There is no child constraint because this kind of node can't have children.
            case None => Left(CardinalityViolation(n.node, n.key, c))
          }
        case _ :: ns0 => deep(c, ns0)
        case Nil => Right(c)
      }

    def existence(c: Constraint, ns: List[TypeTree]): Either[Violation, Constraint] =
      if (c.requiredTypes.forall(t => ns.exists(_.node.ct == t))) Right(c)
      else Left(CardinalityViolation(tree.node, tree.key, c))

    // Validate the children in a shallow manner first, to generate the new
    // constraints that we use to validate the children
    val ns = tree.children
    for {
      c1 <- flat(c, ns).right
      _  <- deep(c1, ns).right
      _  <- existence(c1, ns).right
    } yield c1

  }

}