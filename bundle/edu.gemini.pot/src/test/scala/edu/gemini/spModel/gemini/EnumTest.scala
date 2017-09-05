package edu.gemini.spModel.gemini

import edu.gemini.shared.util.immutable.{ None => GNone, Option => GOption, Some => GSome }
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.junit.Assert._

sealed class EnumTest[T <: Enum[T]](e: Class[T], valueOf: (String, GOption[T]) => GOption[T]) {

  def testValid(defValue: GOption[T]): Unit =
    e.getEnumConstants.foreach { t =>
      assertEquals(s"valueOf ${t.name} failed", new GSome(t), valueOf(t.name, defValue))
    }

  def testValidNone(): Unit =
    testValid(GNone.instance[T])

  def testValidSome(): Unit = {
    testValid(e.getEnumConstants.headOption.asGeminiOpt)
  }

  def testInvalid(defValue: GOption[T]): Unit =
    e.getEnumConstants.map(t => s"_${t.name}").foreach { tag =>
      assertEquals(s"valueOf $tag should return $defValue", defValue, valueOf(tag, defValue))
    }

  def testInvalidNone(): Unit =
    testInvalid(GNone.instance[T])

  def testInvalidSome(): Unit =
    testInvalid(e.getEnumConstants.headOption.asGeminiOpt)

  def testAll(): Unit = {
    testValidNone
    testValidSome
    testInvalidNone
    testInvalidSome
  }
}

object EnumTest {
  def test[T <: Enum[T]](e: Class[T], valueOf: (String, GOption[T]) => GOption[T]): Unit =
    new EnumTest(e, valueOf).testAll
}