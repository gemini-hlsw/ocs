package jsky.app.ot.viewer

import edu.gemini.pot.sp._
import edu.gemini.spModel.util.DBProgramInfo
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import annotation.tailrec
import edu.gemini.spModel.gemini.plan.NightlyRecord
import java.security.Principal

package object open {

  implicit def pimpRoot(p: ISPProgram) = new {

    def getProgInfo(db: IDBDatabaseService) = new DBProgramInfo(
      p.getNodeKey,
      p.getDataObject match {
        case o: SPProgram => o.getTitle
        case p: NightlyRecord => p.getTitle
      },
      p.getProgramID,
      db.fileSize(p.getNodeKey),
      p.lastModified)

    def hasConflicts = {
      def hasConflicts0(n: ISPNode):Boolean = n match {
        case n if n.hasConflicts => true
        case c:ISPContainerNode => c.getChildren.asScala.exists(hasConflicts0)
        case _ => false
      }
      hasConflicts0(p)
    }

  }

  implicit def pimpDatabase(db: IDBDatabaseService) = new {

    def allPrograms(user: java.util.Set[Principal]) = {
      var all = List[ISPProgram]()
      db.getQueryRunner(user).queryPrograms(new DBAbstractQueryFunctor {
        def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]) {
          all = node.asInstanceOf[ISPProgram] :: all
        }
      })
      all
    }

  }

  implicit def pimpAtomicReference[A](ref:AtomicReference[A]) = new {

    @tailrec def modify(f: A => A) {
      val a = ref.get
      if (!ref.compareAndSet(a, f(a)))
        modify(f)
    }

  }


}
