package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.gemini.init.ObservationNI

// Makes empty nodes with matching program and node keys but a default data
// object and no children.
private[vcs] case class EmptyNodeCopier(fact: ISPFactory, existing: ISPProgram) {
  private class NodeFactory(preserveKeys: Boolean) extends ISPProgramVisitor {
    var newNode: ISPNode = null

    private def set(node: ISPNode) {
      node.children = List.empty
      newNode = node
    }

    private def key(n: ISPNode): SPNodeKey =
      if (preserveKeys) n.getNodeKey else new SPNodeKey()

    def visitConflictFolder(node: ISPConflictFolder) {
      set(fact.createConflictFolder(existing, key(node)))
    }
    def visitObsComponent(node: ISPObsComponent) {
      set(fact.createObsComponent(existing, node.getType, key(node)))
    }
    def visitObservation(node: ISPObservation) {
      set(fact.createObservation(existing, -1, ObservationNI.NO_CHILDREN_INSTANCE, key(node)))
    }
    def visitGroup(node: ISPGroup) {
      set(fact.createGroup(existing, key(node)))
    }
    def visitProgram(node: ISPProgram) {
      sys.error("the program node itself should exist in both version of the program")
    }
    def visitSeqComponent(node: ISPSeqComponent) {
      set(fact.createSeqComponent(existing, node.getType, key(node)))
    }
    def visitTemplateFolder(node: ISPTemplateFolder) {
      set(fact.createTemplateFolder(existing, key(node)))
    }
    def visitTemplateGroup(node: ISPTemplateGroup) {
      set(fact.createTemplateGroup(existing, key(node)))
    }
    def visitTemplateParameters(node: ISPTemplateParameters) {
      set(fact.createTemplateParameters(existing, key(node)))
    }
    def visitObsQaLog(node: ISPObsQaLog) {
      set(fact.createObsQaLog(existing, key(node)))
    }
    def visitObsExecLog(node: ISPObsExecLog) {
      set(fact.createObsExecLog(existing, key(node)))
    }
  }

  // If inNode is an ISPProgramNode, creates an empty node of the same type
  // with the same node key and program key, but associated with our
  // ISPFactory's database.
  def apply(inNode: ISPNode, preserveKeys: Boolean = true): Option[ISPNode] =
    inNode match {
      case pn: ISPProgramNode  =>
        // The visitor pattern here is awkward to use.  Could just pattern
        // match on the various types but then there would be no compiler help
        // if we should add a new node type.
        val nf = new NodeFactory(preserveKeys)
        pn.accept(nf)
        Option(nf.newNode)
      case _                   => None
    }
}
