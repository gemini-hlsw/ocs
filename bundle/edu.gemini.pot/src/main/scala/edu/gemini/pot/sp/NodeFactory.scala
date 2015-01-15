package edu.gemini.pot.sp

import edu.gemini.pot.sp.validator.NodeType
import edu.gemini.spModel.gemini.init.ObservationNI

/** Support for science program node generation given only an `SPComponentType`
  * or example `ISPNode`.
  */
object NodeFactory {
  def mkNode(f: ISPFactory, p: ISPProgram, ct: SPComponentType, key: Option[SPNodeKey]): Option[ISPNode] =
    NodeType.forComponentType(ct).map { nt => fact(nt.mf)(f, p, ct, key.orNull) }

  def mkNode(f: ISPFactory, p: ISPProgram, src: ISPNode): ISPNode =
    fact(NodeType.forNode(src).mf)(f, p, src.getDataObject.getType, src.getNodeKey)

  private val fact: Map[Class[_], (ISPFactory, ISPProgram, SPComponentType, SPNodeKey) => ISPNode] = Map(
    classOf[ISPConflictFolder]     -> { (f,p,_,k) => f.createConflictFolder(p, k) },
    classOf[ISPGroup]              -> { (f,p,_,k) => f.createGroup(p, k) },
    classOf[ISPObservation]        -> { (f,p,_,k) => f.createObservation(p, -1, ObservationNI.NO_CHILDREN_INSTANCE, k) },
    classOf[ISPObsComponent]       -> { (f,p,c,k) => f.createObsComponent(p, c, k) },
    classOf[ISPObsExecLog]         -> { (f,p,_,k) => f.createObsExecLog(p, k) },
    classOf[ISPObsQaLog]           -> { (f,p,_,k) => f.createObsQaLog(p, k) },
    classOf[ISPProgram]            -> { (f,p,_,k) => f.createProgram(k, p.getProgramID) },
    classOf[ISPSeqComponent]       -> { (f,p,c,k) => f.createSeqComponent(p, c, k) },
    classOf[ISPTemplateFolder]     -> { (f,p,_,k) => f.createTemplateFolder(p, k) },
    classOf[ISPTemplateGroup]      -> { (f,p,_,k) => f.createTemplateGroup(p, k) },
    classOf[ISPTemplateParameters] -> { (f,p,_,k) => f.createTemplateParameters(p, k) }
  )
}
