package edu.gemini.phase2.template.factory.impl

import edu.gemini.pot.sp.{ISPSeqComponent, ISPObsComponent, ISPObservation, SPComponentType}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqConfigComp

/**
 * Utility for editing an observation and its instrument obs component.
 */
case class ObservationEditor[I <: ISPDataObject](obs: ISPObservation, instType: SPComponentType, iterType: SPComponentType) {
  val staticEditor = StaticObservationEditor(obs, instType)

  def instrument: Maybe[ISPObsComponent] = staticEditor.instrument

  def instrumentDataObject: Maybe[I] = staticEditor.instrumentDataObject

  def updateInstrument(up: I => Unit): Maybe[Unit] = staticEditor.updateInstrument(up)

  def instrumentIterator: Maybe[ISPSeqComponent] =
    obs.findSeqComponentsByType(iterType) match {
      case c :: Nil => Right(c)
      case cs => Left("Observation '%s' has %d instrument iterators (expected 1).".format(obs.libraryId, cs.length))
    }

  def instrumentIterators: Maybe[List[ISPSeqComponent]] =
    Right(obs.findSeqComponentsByType(iterType))

  def instrumentIteratorDataObject(it: ISPSeqComponent): Maybe[SeqConfigComp] =
    for {
      dobj <- it.dataObject.map(_.asInstanceOf[SeqConfigComp]).toRight("Observation '%s' has an instrument iterator but it is empty").right
    } yield dobj


  def iterate(propName: String, values: List[_]): Maybe[Unit] =
    for {
      it   <- instrumentIterator.right
      _ <- iterate0(propName, values)(it).right
    } yield ()

  def iterateAll(propName: String, values: List[_]): Maybe[Unit] =
    for {
      its   <- instrumentIterators.right
      _ <- its.mapM_(iterate0(propName, values)).right
    } yield ()

  def iterateFirst(propName: String, values: List[_]): Maybe[Unit] =
    for {
      its   <- instrumentIterators.right
      _ <- its.headOption.toList.mapM_(iterate0(propName, values)).right
    } yield ()


  private def iterate0(propName: String, values: List[_])(it:ISPSeqComponent): Maybe[Unit] =
    for {
      dobj <- instrumentIteratorDataObject(it).right
      _    <- values.headOption.toRight("Cannot iterate over an empty list of '%s'".format(propName)).right
    } yield {
      val sys = dobj.getSysConfig
      val m   = toMap(sys) + (propName -> values)

      sys.removeParameters()

      // Set the length of all the lists to the length of the longest list,
      // repeating the last value as necessary to fill in the missing entries
      val length = m.values.maxBy(_.size).size
      sys.putParameters(toParams(m mapValues { lst => lst.padTo(length, lst.last) }))

      dobj.setSysConfig(sys)
      it.setDataObject(dobj)
    }



  /** Modify a sequence by examining each step in isolation. */
  private def modifySeq(dobj:SeqConfigComp)(f: Map[String, _] => Maybe[Map[String,  _]]):Maybe[Unit] = {
    val sys = dobj.getSysConfig
    transpose1[String,Any](toMap(sys)).mapM(f).right.map { m2 =>
      sys.removeParameters()
      sys.putParameters(toParams(transpose2[String,Any](m2)))
      dobj.setSysConfig(sys)
    }
  }



  type StepTransformer = Map[String, _] => Maybe[Map[String,  _]]


  /** Modify the sequence by examining each step in isolation. */
  private def modifySeq1(f: StepTransformer)(it:ISPSeqComponent):Maybe[Unit] =
    for {
      dobj <- instrumentIteratorDataObject(it).right
      _ <- modifySeq(dobj)(f).right
    } yield it.setDataObject(dobj)




  /** Modify the first iterator. */
  def modifySeq(f: StepTransformer):Maybe[Unit] =
    for {
      it   <- instrumentIterator.right
      dobj <- instrumentIteratorDataObject(it).right
      _ <- modifySeq(dobj)(f).right
    } yield it.setDataObject(dobj)



  /** Modify all iterators. */
  def modifySeqAll(f: StepTransformer):Maybe[Unit] = for {
    its <- instrumentIterators.right
    _ <- its.mapM_(modifySeq1(f)).right
  } yield ()

  /** Modify a sequence recursively, replacing a given key's value. */
  def modifySeqAllKey(key:String)(f: PartialFunction[Any, Any]) = modifySeqAll { s =>
    Right(s.get(key) match {
      case Some(v) if f.isDefinedAt(v) => s + (key -> f(v))
      case _ => s
    })
  }

  def modifySteps = new {
    def forIterator(i:Int)(t:StepTransformer):Maybe[Unit] = for {
      its <- instrumentIterators.right
      it <- its.lift(i).toRight("No instrument iterator at index %d.".format(i)).right
      _ <- modifySeq1(t)(it).right
    } yield ()
  }


}
