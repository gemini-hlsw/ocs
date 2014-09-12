package edu.gemini.pit

import java.util.logging.{Logger, Level}

package object catalog {

  val Log = Logger.getLogger(this.getClass.getPackage.getName)

  type Callback = Result => Unit

  implicit class StringPimp(val s:String) extends AnyVal {
    def toDoubleOption = try {
      Some(s.toDouble)
    } catch {
      case _ : NumberFormatException => None
    }
  }

  import scala.language.implicitConversions

  implicit def pimpSideEffect[A](f: A => Unit) = new SafeSideEffect[A] {
    def safe = (a: A) =>
      try {
        f(a)
      } catch {
        case t: Throwable => Log.log(Level.WARNING, "Trouble invoking callback function.", t)
      }

  }

}

package catalog {

  trait SafeSideEffect[A] {
    def safe: A => Unit
  }

}