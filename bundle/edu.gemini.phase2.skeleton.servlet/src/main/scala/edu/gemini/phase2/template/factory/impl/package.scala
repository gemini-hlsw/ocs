package edu.gemini.phase2.template.factory

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.rich.pot.sp._
import scala.collection.JavaConverters._
import edu.gemini.spModel.data.config.{DefaultParameter, IParameter, ISysConfig}

package object impl {

  type Maybe[A] = Either[String,A]
  
  type LibraryId = String
  type LibraryNumber = Int

  def transpose1[A,B](map:Map[A, List[B]]):List[Map[A, B]] = {
    val ws = map.values.map(_.length).toList.distinct
    require(ws.length == 1, "Values are non-uniform.")
    (0 to ws.head - 1).toList.map(n => map.mapValues(_(n)))
  }

  def transpose2[A,B](ms:List[Map[A,B]]):Map[A,List[B]] = {
    val ks = ms.map(_.keys).distinct
    require(ks.length == 1, "Keys are non-uniform.")
    ks.head.map(k => (k -> ms.map(_(k)))).toMap
  }

  // Normalizes a sys config to a Map of non-empty Lists
  def toMap(sys: ISysConfig): Map[String, List[Any]] = {
    def nonEmptyList(p: IParameter): Option[List[Any]] =
      Option(p.getValue) flatMap {
        _ match {
          case c: java.util.Collection[_] => if (c.isEmpty) None else Some(c.asScala.toList)
          case x => Some(List(x))
        }
      }

    (Map.empty[String, List[Any]]/:sys.getParameters.asScala) { case (m, p) =>
      val entry = for {
        name   <- Option(p.getName)
        values <- nonEmptyList(p)
      } yield (name -> values)
      entry.map(e => m+e).getOrElse(m)
    }
  }

  def toParams(m: Map[String, List[Any]]): java.util.Collection[IParameter] = {
    val params = m map {
      case (name, lst) => DefaultParameter.getInstance(name, new java.util.ArrayList[Any](lst.asJavaCollection)).asInstanceOf[IParameter]
    }
    params.asJavaCollection
  }


  implicit def pimpSeq[A](as:Seq[A]) = new {

    // RCN: note that mapM needs to fold right and mapM_ needs to fold left

    /** Returns successful map or first error. */
    def mapM[B, C](f:A => Either[B, C]):Either[B, List[C]] =
      (as :\ (Right(Nil):Either[B, List[C]])) {
        (a, ecs) =>
          for {
            cs <- ecs.right
            c <- f(a).right
          } yield c :: cs
      }

    /** Returns unit or first error. */
    def mapM_[B,C](f:A => Either[B, C]):Either[B, Unit] = {
      val x = ((Right(Nil):Either[B, List[C]]) /: as) {
        (ecs, a) =>
          for {
            cs <- ecs.right
            c <- f(a).right
          } yield c :: cs
      }
      x.right.map(_ => ())
    }

  }

  /** Returns a or result of exception mapping */
  def tryFold[A, B](b: => B)(f:Exception => A):Either[A, B] =
    try Right(b) catch { case e:Exception => Left(f(e)) }


  implicit def obsLookup(grp:ISPGroup) = new Object {

    def apply(libraryId:LibraryId):Maybe[ISPObservation] =
      grp.findObservation(_.libraryId.exists(_ == libraryId)).toRight("Could not find observation '%s' in group '%s'".format(libraryId, grp.libraryId))

    def apply(libraryId:LibraryNumber):Maybe[ISPObservation] = apply(libraryId.toString)

    def allObservations:List[ISPObservation] = grp.getAllObservations.asScala.toList

  }

  def attempt[A](a: => A) = tryFold(a) {e =>
    e.printStackTrace()
    e.getMessage
  }


}