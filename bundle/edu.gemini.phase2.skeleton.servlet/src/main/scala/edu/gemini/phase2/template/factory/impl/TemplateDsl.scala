package edu.gemini.phase2.template.factory.impl

import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqConfigComp
import edu.gemini.pot.sp.{SPComponentType, ISPSeqComponent, ISPGroup, ISPObservation}
import edu.gemini.spModel.core.SPProgramID

trait TemplateDsl {gi:GroupInitializer[_] =>

  sealed trait NoteLocation
  sealed trait ObsLocation

  case object TargetGroup extends ObsLocation with NoteLocation
  case object BaseCal extends ObsLocation with NoteLocation
  case object TopLevel extends NoteLocation
  type Mutator = ISPObservation => Maybe[Unit]

  private val mutators: collection.mutable.Buffer[(Either[Seq[Int], ObsLocation], Mutator)] =
    collection.mutable.Buffer.empty

  private var includes: Map[ObsLocation, Seq[Int]] =
    Map.empty

  private var noteIncludes: Map[NoteLocation, Seq[String]] =
    Map.empty

  def notes: Seq[String] =
    noteIncludes.values.toList.flatten

  def baselineFolder: Seq[Int] =
    includes.getOrElse(BaseCal, Nil)

  def targetGroup: Seq[Int] =
    includes.getOrElse(TargetGroup, Nil)

  def initialize(g:ISPGroup, db:TemplateDb, pid: SPProgramID):Maybe[Unit] =
    mutators.mapM_((mutate(g) _).tupled)

  private def mutate(g:ISPGroup)(e:Either[Seq[Int], ObsLocation], f:Mutator):Maybe[Unit] =
    e match {
      case Left(ns) => mutate(g, ns, f)
      case Right(x) => mutate(g, includes.getOrElse(x, Nil), f)
    }

  private def mutate(g:ISPGroup, ns:Seq[Int], f:Mutator):Maybe[Unit] =
    ns.mapM(g.apply).right.flatMap(_.mapM_(f))

  // OBS, NOTE INCLUDES

  def include(ns: Int*) = new {
    def in(g: ObsLocation): Unit = {
      val prev = includes.getOrElse(g, Nil)
      includes = includes + (g -> (prev ++ ns))
    }
  }

  def addNote(ns:String*) = new {
    def in(g:NoteLocation) {
      val prev = noteIncludes.getOrElse(g, Nil)
      noteIncludes = noteIncludes + (g -> (prev ++ ns))
    }
  }

  // GROUP MUTATION

  def forObs(ns:Int*)(fs:Mutator*) {
    fs foreach {f => mutators.append((Left(ns), f))}
  }

  def forGroup(g:ObsLocation)(fs:Mutator*) {
    fs foreach {f => mutators.append((Right(g), f))}
  }

  // SETTERS

  trait Setter[A] {
    def apply(a:A)(o:ISPObservation):Maybe[Unit]
    def fromPI:Mutator
  }

  object Setter {
    def apply[A](piValue: => A)(f:(ISPObservation, A) => Maybe[Unit]):Setter[A] = new Setter[A] {
      def apply(a:A)(o:ISPObservation) = f(o, a)
      def fromPI = apply(piValue) _
    }
  }

  // CONDITIONAL

  def ifTrue[A, B](b: => Boolean)(fs:(A => Either[B, _])*)(a:A) = if (b) fs.mapM_(_(a)) else Right(())

  // SEQUENCE MUTATION


  def seqConfigCompType:SPComponentType

  type Step = Map[String, Any]
  type Sequence = List[Step]

  trait SeqConfigEditor {
    def forEach(f:Sequence => Maybe[Sequence])(o:ISPObservation):Maybe[Unit]
    def forEachStep(f:Step => Maybe[Step]): Mutator
    def forKey(key:String)(pf:PartialFunction[Any, Any]): Mutator
  }

  private def mod(f:Sequence => Maybe[Sequence])(c:ISPSeqComponent):Maybe[Unit] = {
    val dobj = c.getDataObject.asInstanceOf[SeqConfigComp]
    val sys = dobj.getSysConfig
    val steps = transpose1(toMap(sys))
    for {
      seq0 <- f(steps).right
    } yield {
      sys.removeParameters()
      sys.putParameters(toParams(transpose2(seq0)))
      dobj.setSysConfig(sys)
      c.setDataObject(dobj)
    }
  }

  def modifySeqConfig:SeqConfigEditor = modifySeqConfig(_ => true)

  def modifySeqConfig(names:String*):SeqConfigEditor =
    modifySeqConfig { p: SeqConfigComp =>  names.contains(p.getTitle) }

  def modifySeqConfig(t:SPComponentType):SeqConfigEditor = modifySeqConfig(_.getType == t)

  def modifySeqConfig(p: SeqConfigComp => Boolean):SeqConfigEditor = new SeqConfigEditor {

    private def comps(o:ISPObservation) = o.getSeqComponent.flatten.filter(_.getType == seqConfigCompType)
    private def dataObj(c:ISPSeqComponent) = c.getDataObject.asInstanceOf[SeqConfigComp]
    private def forAll(f:Sequence => Maybe[Sequence])(cs:List[ISPSeqComponent]):Maybe[Unit] = cs.mapM_(mod(f))

    def forEach(f:Sequence => Maybe[Sequence])(o:ISPObservation) =
      forAll(f)(comps(o).filter { node => p(dataObj(node)) })

    def forEachStep(f:Step => Maybe[Step]) = forEach(_.mapM(f)) _

    def forKey(key:String)(f:PartialFunction[Any, Any]) = forEachStep {s:Step =>
      Right(s.map {
        case (k, v) if k == key && f.isDefinedAt(v) => (k, f(v))
        case kv => kv
      })
    }

  }


}
