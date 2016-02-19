package edu.gemini.model.p1.dtree

import scala.annotation.tailrec

/**
 * A textual UI for testing decision trees, with support for undo, redo, and selection of default
 * choices following undo. Just call run() with a node or UIState.
 */
object TextUI {

  // When we ask the user what to do next, these are the options.
  private sealed trait Input[+A]
  private case object Undo extends Input[Nothing]
  private case object Redo extends Input[Nothing]
  private case class Choice[A](a: A) extends Input[A]
  private case class Text[A](a: A) extends Input[A]

  // Prompt the user and return an Input of the appropriate selection type, given a UIState.
  @tailrec
  private def input[C, S](state: UIState[C, S]): Input[S] = {

    // Interpret an input string with respect to the UIState, attempting to turn it into a
    // selection of the appropriate type.
    def interp(s: String): Option[S] = s match {
      case "" => state.default
      case t => try {
        state match {
          case state: SelectUIState[C, S] => state.select(t.split("\\D+").toList.map(_.toInt - 1))
          case state: TextUIState[C]      => state.text(t)
        }
      } catch {
        case _: NumberFormatException => None
      }
    }
    // Build a prompt
    val sb = new StringBuilder(state.node match {
      case n:SelectNode[_, _, _, _] => f"Choose 1-${n.choices.length}"
      case _                        => "Enter text [abc..]"
    })
    state.defaultString.map(" " + _).foreach(sb.append)
    if (state.canUndo) sb.append(", undo")
    if (state.canRedo) sb.append(", redo")
    sb.append(": ")

    // Read and return, or try again
    scala.io.StdIn.readLine(sb.toString()).trim.toLowerCase match {
      case "undo" if state.canUndo => Undo
      case "redo" if state.canRedo => Redo
      case s => interp(s) match { // n.b. map + getOrElse makes tail call optimization fail
        case Some(u)  => Choice(u)
        case None     => input(state)
      }
    }

  }

  /** Run with the specified UIState, returning the final result. */
  // @tailrec -- fails
  def run[C, S](state: UIState[C, S]): (UIState[_,_], Any) = {

    // Print our prompt and choices
    println(s"\n*** ${state.node.title}")
    state.node.description.trim.lines.map(_.trim).foreach(println)
    state.node match {
      case n:SelectNode[_, _, _, _] => n.choices.zipWithIndex.foreach {
          case (c, p) =>
            print(s" ${p+1}. $c")
            state.default.filter(_ == c).foreach(_ => print(" (*)"))
            println()
        }
      case _ =>
    }

    // Interpret user input
    input(state) match {
      case Undo      => run(state.undo.get) // it's a logic problem if this ever fails
      case Redo      => run(state.redo.get) // ditto
      case Choice(c) => state(c) match {
        case Left(s)  => run(s) // N.B. tailcall optimization fails here
        case Right(r) => r
      }
      case Text(c)   => state(c) match {
        case Left(s)  => run(s) // N.B. tailcall optimization fails here
        case Right(r) => r
      }
    }

  }
  

}


