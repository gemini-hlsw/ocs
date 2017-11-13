package edu.gemini.phase2.template.factory.impl

import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.pot.sp._
import edu.gemini.spModel.seqcomp.SeqConfigComp
import edu.gemini.spModel.gemini.altair.blueprint.{SpAltairNgs, SpAltairLgs, SpAltair}
import edu.gemini.spModel.gemini.altair.AltairParams.{FieldLens, Mode}
import edu.gemini.spModel.gemini.altair.InstAltair
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset
import scala.collection.JavaConverters._
import scala.collection.{mutable => m}

trait TemplateDsl2[I <: SPInstObsComp] { gi:GroupInitializer[_] =>

  // Type aliases
  type Mutator = ISPObservation => Maybe[Unit]
  type StaticComponentMutator = I => Maybe[Unit]
  type Step = Map[String, Any]
  type Sequence = List[Step]
  type SequenceMutator = Sequence => Sequence
  type StepMutator = Step => Step

  // Abstract members
  def instCompType:SPComponentType
  def seqConfigCompType:SPComponentType
  def db: Option[TemplateDb] // this is a hack, sorry

  sealed trait NoteLocation
  sealed trait ObsLocation

  case object TargetGroup extends ObsLocation with NoteLocation
  case object BaseCal extends ObsLocation with NoteLocation
  case object TopLevel extends NoteLocation

  private val mutators: m.Buffer[(Either[Seq[Int], ObsLocation], Mutator)] = m.Buffer.empty
  private var includes:Map[ObsLocation, Seq[Int]] = Map.empty
  private var noteIncludes:Map[NoteLocation, Seq[String]] = Map.empty

  def curIncludes(loc: ObsLocation): Seq[Int] =
    includes.getOrElse(loc, Seq.empty)

  def notes = noteIncludes.values.toList.flatten
  def baselineFolder = includes.get(BaseCal).getOrElse(Nil)
  def targetGroup = includes.get(TargetGroup).getOrElse(Nil)

  def initialize(g:ISPGroup, db:TemplateDb):Maybe[Unit] = mutators.mapM_((mutate(g) _).tupled)

  private def mutate(g:ISPGroup)(e:Either[Seq[Int], ObsLocation], f:Mutator):Maybe[Unit] = e match {
    case Left(ns) => mutate(g, ns, f)
    case Right(x) => mutate(g, includes.get(x).getOrElse(Nil), f)
  }

  private def mutate(g:ISPGroup, ns:Seq[Int], f:Mutator):Maybe[Unit] =
    ns.mapM(g.apply).right.flatMap(_.mapM_(f))

  // OBS, NOTE INCLUDES

  def include(ns:Int*) = new {
    def in(g:ObsLocation) {
      val prev = includes.get(g).getOrElse(Nil)
      includes = includes + (g -> (prev ++ ns))
    }
  }

  def addNote(ns:String*) = new {
    def in(g:NoteLocation) {
      val prev = noteIncludes.get(g).getOrElse(Nil)
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

  // STATIC COMPONENT MUTATION

  private def mutateStaticComponent(f:StaticComponentMutator)(o:ISPObservation):Maybe[Unit] = for {
    iNode <- o.findObsComponentByType(instCompType).toRight(errMsg(o, "No instrument of type '%s'".format(instCompType.readableStr))).right
    iData <- iNode.dataObject.map(_.asInstanceOf[I]).toRight("impossible").right
    _ <- f(iData).right
  } yield iNode.setDataObject(iData)

  // def mutateFoo = mutateStatic[Foo](_.setFoo(_)) ... mutateFoo(myFoo) is a Mutator
  def mutateStatic[A](f:(I, A) => Unit) = (a:A) => mutateStaticComponent(o => attempt(f(o, a))) _

  // SEQUENCE MUTATION

  def mutateSeq = new {

    private def comps(o:ISPObservation) = o.getSeqComponent.flatten.filter(_.getType == seqConfigCompType)
    private def dataObj(c:ISPSeqComponent) = c.getDataObject.asInstanceOf[SeqConfigComp]

    private def mod(f:SequenceMutator)(c:ISPSeqComponent):Maybe[Unit] = attempt {
      val dobj = c.getDataObject.asInstanceOf[SeqConfigComp]
      val sys = dobj.getSysConfig
      val steps = transpose1(toMap(sys))
      val seq0 = f(steps)
      sys.removeParameters()
      sys.putParameters(toParams(transpose2(seq0)))
      dobj.setSysConfig(sys)
      c.setDataObject(dobj)
    }

    def atIndex(i:Int)(f:SequenceMutator) = (o:ISPObservation) => for {
      c <- comps(o).lift(i).toRight(errMsg(o, "No sequence at index %d.".format(i))).right
      _ <- mod(f)(c).right
    } yield ()

    def withTitle(title:String)(f:SequenceMutator) = (o:ISPObservation) => for {
      c <- comps(o).find(dataObj(_).getTitle == title).toRight(errMsg(o, "No sequence with title %s.".format(title))).right
      _ <- mod(f)(c).right
    } yield ()

    def withTitleIfExists(title: String)(f: SequenceMutator) = (o: ISPObservation) =>
      comps(o).find(dataObj(_).getTitle == title).fold(Right(()): Maybe[Unit])(mod(f)(_))

    def apply(f:SequenceMutator) = (o:ISPObservation) => comps(o).mapM_(mod(f))

  }

  def iterate[A](param:String, as:Seq[A]):SequenceMutator = { steps =>
    require(as.length >= steps.length, "Too many steps to iterate.")
    steps.padTo(as.length, steps.last).zip(as).map {
      case (step, f) => step + (param -> f)
    }
  }

  def mapSteps(f:StepMutator):SequenceMutator = _.map(f)

  def mapStepsByKey(param:String)(pf:PartialFunction[Any,Any]):SequenceMutator = mapSteps {
    _.map {
      case (k, v) if k == param && pf.isDefinedAt(v) => (k, pf(v))
      case kv => kv
    }
  }


  // OFFSET MUTATION

  type OffsetMutator = SeqRepeatOffset => SeqRepeatOffset

  def mutateOffsets = new {

    private def comps(o:ISPObservation) = o.getSeqComponent.flatten.filter(_.getType == SeqRepeatOffset.SP_TYPE)
    private def dataObj(c:ISPSeqComponent) = c.getDataObject.asInstanceOf[SeqRepeatOffset]

    private def mod(f:OffsetMutator)(c:ISPSeqComponent) = attempt {
      c.setDataObject(f(dataObj(c)))
    }

    def atIndex(i:Int)(f:OffsetMutator) = (o:ISPObservation) => for {
      c <- comps(o).lift(i).toRight(errMsg(o, "No offset iterator at index %d.".format(i))).right
      _ <- mod(f)(c).right
    } yield ()

    def withTitle(title:String)(f:OffsetMutator) = (o:ISPObservation) => for {
      c <- comps(o).find(dataObj(_).getTitle == title).toRight(errMsg(o, "No offset iterator with title %s.".format(title))).right
      _ <- mod(f)(c).right
    } yield ()

    def apply(f:OffsetMutator) = (o:ISPObservation) => comps(o).mapM_(mod(f))

  }

  def setQ(ds: Double*):OffsetMutator = { o =>
    val ps = o.getPosList.getAllPositions.asScala
    require(ps.length == ds.length, "Position list is the wrong length.")
    ps.zip(ds).foreach {
      case (p, d) => p.setYAxis(d)
    }
    o
  }

  // CONDITIONAL

  def ifTrue[A, B](b: => Boolean)(fs:(A => Either[B, _])*)(a:A) = if (b) fs.mapM_(_(a)) else Right(())

  // ALTAIR SUPPORT

  implicit def pimpSpAltair(a: SpAltair) = new {
    def mode = a match {
      case lgs: SpAltairLgs => if (lgs.usePwfs1) Some(Mode.LGS_P1) else Some(Mode.LGS)
      case ngs: SpAltairNgs => if (ngs.fieldLens == FieldLens.IN) Some(Mode.NGS_FL) else Some(Mode.NGS)
      case _ => None
    }
  }

  implicit def pimpAltairMode(m: Mode) = new {
    def isNGS = m == Mode.NGS || m == Mode.NGS_FL
    def isLGS = m == Mode.LGS || m == Mode.LGS_P1
  }

  def addAltair(m: Mode)(o: ISPObservation): Maybe[Unit] =
    o.findObsComponentByType(InstAltair.SP_TYPE) match {

      // Ok sometimes there's already a altair component.
      case Some(oc) =>
        val altair = oc.getDataObject.asInstanceOf[InstAltair]
        altair.setMode(m)
        Right(oc.setDataObject(altair))

      case None =>
        val oc = db.get.odb.getFactory.createObsComponent(o.getProgram, InstAltair.SP_TYPE, null)
        val altair = new InstAltair
        altair.setMode(m)
        oc.setDataObject(altair)
        Right(o.addObsComponent(oc))

    }

  private def errMsg(o:ISPObservation, s:String) = "%s [libraryID = %s]: %s".format(program, o.libraryId.orNull, s)

}

