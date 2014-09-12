package edu.gemini.model.p1.targetio.impl

import org.junit.Assert._
import org.junit.Test
import edu.gemini.model.p1.targetio.table.{Column, TableReader}
import edu.gemini.model.p1.targetio.api.{ParseError, TargetReader}
import edu.gemini.model.p1.immutable.Target

abstract class ReaderTestBase[T <: Target](val rdr: TargetReader[T]) {

  protected def unrecognized(input: String) {
    rdr.read(input) match {
      case Left(err) => assertEquals(TableReader.UNRECOGNIZED_MESSAGE, err.msg)
      case Right(_)  => fail()
    }
  }

  @Test def testUnrecognized() {
    unrecognized("")
    unrecognized("foo")
  }

  protected def empty(input: String) {
    rdr.read(input) match {
      case Left(err)  => fail(err.msg)
      case Right(lst) => assertEquals(0, lst.size)
    }
  }

  protected def missing(cols: List[String], input: String) {
    rdr.read(input) match {
      case Left(err) => assertEquals(TableReader.MISSING_COLUMNS_MESSAGE(cols), err.msg)
      case _         => fail()
    }
  }

  protected  def onebadrow(col: Column[_,_], badData: String, input: String) {
    rdr.read(input) match {
      case Left(err)  => fail(err.msg)
      case Right(lst) =>
        lst match {
          case Left(ParseError(msg, _, _)) :: Nil =>
            assertEquals(col.parseError(badData), msg)
          case _ => fail()
        }
    }
  }

  protected def good(expected: List[T], input: String) {
    rdr.read(input) match {
      case Left(err)  => fail(err.msg)
      case Right(lst) =>
        val (lefts, rights) = lst.partition(_.isLeft)
        lefts.map(_.left.get.msg) foreach { msg => fail(msg) }
        validateTargets(expected, rights map { e => e.right.get })
    }
  }

  protected def good(expected: T, input: String) {
    good(List(expected), input)
  }

  protected def validateTargets(expected: List[T], actual: List[T]) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual) foreach {
      case (ex, ac) => validateTarget(ex, ac)
    }
  }

  protected def validateTarget(expected: T, actual: T)
}