package edu.gemini.model.p1.dtree

import edu.gemini.model.p1.{ immutable => I }

import scalaz._
import Scalaz._

import scala.language.existentials

object Test extends App {

  val node = new Root(I.Semester(2012, I.SemesterOption.B))
  val (choices, result) = TextUI.run(node.toUIPage.toInitialState)
  println("Choices were " + choices)
  println("Result was " + result)

  //  val list = node.recoverState(result).get

  println("Recoving state.")

  val recovered = node.toUIPage.toInitialState.recover(result).get

  println(choices)
  println(recovered)

  val (choices0, result0) = TextUI.run(recovered)
  println("Choices were " + choices0)
  println("Result was " + result0)

}


