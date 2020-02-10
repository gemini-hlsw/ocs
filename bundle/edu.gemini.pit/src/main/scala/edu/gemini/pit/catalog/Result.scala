package edu.gemini.pit.catalog

import edu.gemini.model.p1.immutable.{NonSiderealTarget, SiderealTarget, Target}

import scala.concurrent.ExecutionContext


// Catalog lookup returns a result, which has four shapes
sealed trait Result
sealed trait Failure extends Result

// On sucessful lookup you get a list of targets and a list of choices; typically one will be empty and the other won't
// but if you && several catalogs together you might get both.
case class Success(targets: List[Target], choices: List[Choice]) extends Result

// There are three failure results
case class NotFound(id: String) extends Failure
case class Error(t: Throwable) extends Failure
case object Offline extends Failure

// A choice represents an option the user can select (in the case of an ambiguous lookup). Once the user has selected
// a choice, you apply it using the same callback that was used for the initial lookup.
case class Choice(cat: Catalog, name: String, id: String) {

  def apply(callback: Callback)(implicit ex: ExecutionContext): Unit = {
    val f = callback.safe
    cat.find(id) onComplete {
      case scala.util.Success(Success(t :: Nil, Nil)) =>
        f(Success(List(withName(t, name)), Nil))
      case scala.util.Success(r)                      =>
        f(r)
      case scala.util.Failure(t)                      =>
        f(Error(t))
    }
  }

  def withName(t: Target, name: String) = t match {
    case t: SiderealTarget => t.copy(name = name)
    case t: NonSiderealTarget => t.copy(name = name)
    case _ => sys.error("Impossible!") // should never happen
  }

}

