package edu.gemini.spModel.io.impl

import edu.gemini.shared.util.immutable.{ApplyOp, DefaultImList, None=>JNone, Option=>JOption, Some=>JSome}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.pio.{Pio, ParamSet, PioFactory}
import edu.gemini.pot.sp._
import edu.gemini.pot.sp.Conflict._

import scala.collection.JavaConverters._

/**
 * ParamSet IO for Conflicts and Conflict.Note.
 */
object ConflictPio {
  val conflictsParamSet          = "conflicts"
  val dataObjectConflictParamSet = "dataObjectConflict"
  val nodeParam                  = "node"
  val noteKind                   = "note"
  val perspectiveParam           = "perspective"

  sealed trait NoteIo[N <: Conflict.Note] {
    def name: String
    def mkNote(ps: ParamSet): N
    def mkParamSet(f: PioFactory, note: N): ParamSet = {
      val ps = f.createParamSet(name)
      Pio.addParam(f, ps, nodeParam, note.getNodeKey.toString)
      ps.setKind(noteKind)
      initParamSet(f, ps, note)
      ps
    }

    /** Override for any extra initialization that might be required. */
    protected def initParamSet(f: PioFactory, ps: ParamSet, note: N) { /* blank */ }
  }

  case object NoteIo {
    private def key(ps: ParamSet, paramName: String): SPNodeKey =
      new SPNodeKey(Pio.getValue(ps, paramName))
    private def nodeKey(ps: ParamSet): SPNodeKey = key(ps, nodeParam)

    case object MovedIo extends NoteIo[Moved] {
      def name = "moved"
      def mkNote(ps: ParamSet) = new Moved(nodeKey(ps), key(ps, "to"))
      override def initParamSet(f: PioFactory, ps: ParamSet, note: Moved) {
        Pio.addParam(f, ps, "to", note.getDestinationKey.toString)
      }
    }

    case object ResurrectedLocalDeleteIo extends NoteIo[ResurrectedLocalDelete] {
      def name = "resurrectedLocalDelete"
      def mkNote(ps: ParamSet) = new ResurrectedLocalDelete(nodeKey(ps))
    }

    case object ReplacedRemoteDeleteIo extends NoteIo[ReplacedRemoteDelete] {
      def name = "replacedRemoteDelete"
      def mkNote(ps: ParamSet) = new ReplacedRemoteDelete(nodeKey(ps))
    }

    case object CreatePermissionFailIo extends NoteIo[CreatePermissionFail] {
      def name = "createPermissionFail"
      def mkNote(ps: ParamSet) = new CreatePermissionFail(nodeKey(ps))
    }

    case object UpdatePermissionFailIo extends NoteIo[UpdatePermissionFail] {
      def name = "updatePermissionFail"
      def mkNote(ps: ParamSet) = new UpdatePermissionFail(nodeKey(ps))
    }

    case object DeletePermissionFailIo extends NoteIo[DeletePermissionFail] {
      def name = "deletePermissionFail"
      def mkNote(ps: ParamSet) = new DeletePermissionFail(nodeKey(ps))
    }

    case object ConstraintViolationIo extends NoteIo[ConstraintViolation] {
      def name = "constraintViolation"
      def mkNote(ps: ParamSet) = new ConstraintViolation(nodeKey(ps))
    }

    case object ConflictFolderIo extends NoteIo[ConflictFolder] {
      def name = "conflictFolder"
      def mkNote(ps: ParamSet) = new ConflictFolder(nodeKey(ps))
    }

    val all: List[NoteIo[_ <: Conflict.Note]] = List(
      MovedIo,
      ResurrectedLocalDeleteIo,
      ReplacedRemoteDeleteIo,
      CreatePermissionFailIo,
      UpdatePermissionFailIo,
      DeletePermissionFailIo,
      ConstraintViolationIo,
      ConflictFolderIo
    )

    // Using a visitor to guarantee that we handle this case when we add new
    // note types.
    final class ParamSetFactory(f: PioFactory) extends NoteVisitor {
      var ps: ParamSet = null

      override def visitMoved(note: Moved)                               { ps = MovedIo.mkParamSet(f,note)                }
      override def visitResurrectedLocalDelete(note: ResurrectedLocalDelete)  { ps = ResurrectedLocalDeleteIo.mkParamSet(f,note)  }
      override def visitReplacedRemoteDelete(note: ReplacedRemoteDelete) { ps = ReplacedRemoteDeleteIo.mkParamSet(f,note) }
      override def visitCreatePermissionFail(note: CreatePermissionFail) { ps = CreatePermissionFailIo.mkParamSet(f,note) }
      override def visitUpdatePermissionFail(note: UpdatePermissionFail) { ps = UpdatePermissionFailIo.mkParamSet(f,note) }
      override def visitDeletePermissionFail(note: DeletePermissionFail) { ps = DeletePermissionFailIo.mkParamSet(f,note) }
      override def visitConstraintViolation(note: ConstraintViolation)   { ps = ConstraintViolationIo.mkParamSet(f,note)}
      override def visitConflictFolder(note: ConflictFolder)             { ps = ConflictFolderIo.mkParamSet(f,note)       }
    }
  }

  def toParamSet(f: PioFactory, doc: DataObjectConflict): ParamSet = {
    val ps = f.createParamSet(dataObjectConflictParamSet)
    Pio.addParam(f, ps, perspectiveParam, doc.perspective.name)
    ps.addParamSet(doc.dataObject.asInstanceOf[ISPDataObject].getParamSet(f))
    ps
  }

  def toParamSet(f: PioFactory, cn: Conflict.Note): ParamSet = {
    val psf = new NoteIo.ParamSetFactory(f)
    cn.accept(psf)
    psf.ps
  }

  def toParamSet(f: PioFactory, c: Conflicts): ParamSet = {
    val ps = f.createParamSet(conflictsParamSet)
    ps.setKind(conflictsParamSet)

    c.dataObjectConflict foreach { new ApplyOp[DataObjectConflict] {
      def apply(doc: DataObjectConflict) { ps.addParamSet(toParamSet(f, doc)) }
    }}

    if (c.notes.size() > 0) {
      c.notes.toList.asScala foreach { note =>
        ps.addParamSet(toParamSet(f, note))
      }
    }

    ps
  }

  private def findChildren(parent: ParamSet, kind: String): List[ParamSet] =
    parent.getParamSets.asScala.toList filter { _.getKind == kind }

  private def findChild(parent: ParamSet, kind: String): Option[ParamSet] =
    findChildren(parent, kind).headOption

  def parseDataObjectConflictPerspective(s: String): Either[String, DataObjectConflict.Perspective] =
    try {
      Right(DataObjectConflict.Perspective.valueOf(s))
    } catch {
      case _: Exception => Left("Unknown perspective: " + s)
    }

  def parseDataObjectConflict(ps: ParamSet, n: ISPNode): Either[String, DataObjectConflict] =
    for {
      dbjParamSet <- findChild(ps, ISPDataObject.PARAM_SET_KIND).toRight("DataObjectConflict missing its dataObj").right
      perString   <- Option(Pio.getValue(ps, perspectiveParam)).toRight("DataObjectConflict missing its perspective").right
      perspective <- parseDataObjectConflictPerspective(perString).right
    } yield {
      // this is a bit grim ... the program node is needed just to get a data
      // object instance of the right type -- the fields are just reset
      // immediately by the call to setParamSet
      val dbj = n.getDataObject.asInstanceOf[ISPDataObject]
      dbj.setParamSet(dbjParamSet)
      new DataObjectConflict(perspective, dbj)
    }

  def parseConflictNote(ps: ParamSet): Either[String, Conflict.Note] =
    NoteIo.all.find(_.name == ps.getName).toRight("Could not find conflict note '%s'".format(ps.getName)).right map { _.mkNote(ps) }

  def parseConflicts(ps: ParamSet, n: ISPNode): Either[String, Conflicts] = {
    val dataObjectConflict: Either[String, JOption[DataObjectConflict]] = {
      Option(ps.getParamSet(dataObjectConflictParamSet)) map { docPs =>
        parseDataObjectConflict(docPs, n).right map { doc => new JSome(doc) }
      } getOrElse {
        Right(JNone.instance[DataObjectConflict])
      }
    }

    val conflictNotes: Either[String, List[Conflict.Note]] = {
      val z: Either[String, List[Conflict.Note]] = Right(Nil)
      (findChildren(ps, noteKind):\z) { (notePs, e) =>
        for {
          lst <- e.right
          cn  <- parseConflictNote(notePs).right
        } yield cn :: lst
      }
    }

    for {
      doc   <- dataObjectConflict.right
      notes <- conflictNotes.right
    } yield Conflicts.apply(doc, DefaultImList.create(notes.asJava))
  }

  def toConflicts(ps: ParamSet, n: ISPNode): Conflicts =
    parseConflicts(ps, n).fold(msg => sys.error(msg), identity)
}