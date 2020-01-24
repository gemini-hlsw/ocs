package edu.gemini.shared.util

import java.lang.{Integer => JInt}

/**
 * Provides the Integral type class implementation for java.lang.Integer,
 * which is useful when combining Scala and Java.
 */
trait IntegerIsIntegral extends Integral[JInt] with Ordering[JInt] {

  def compare(x: JInt, y: JInt): Int = x.compareTo(y)

  // x + y is fine because Integer2int in Predef does the conversion to scala
  // Int and the return value is converted back by int2Integer.  That works
  // from the command line and builds fine in Idea, but the scala Idea plugin
  // incorrectly flags it as an error so I explicitly used x.intValue instead.
  def plus(x: JInt, y: JInt)   = x.intValue() + y.intValue()

  def minus(x: JInt, y: JInt)  = x.intValue() - y.intValue()
  def times(x: JInt, y: JInt)  = x.intValue() * y.intValue()
  def negate(x: JInt)          = -x.intValue()
  def fromInt(x: Int)          = x
  def toInt(x: JInt)           = x
  def toLong(x: JInt)          = x.intValue().toLong
  def toFloat(x: JInt)         = x.intValue().toFloat
  def toDouble(x: JInt)        = x.intValue().toDouble
  def quot(x: JInt, y: JInt)   = x.intValue() / y.intValue()
  def rem(x: JInt, y: JInt)    = x.intValue() % y.intValue()
  def parseString(str: String): Option[JInt] =
    try {
      Some(JInt.parseInt(str))
    } catch {
      case _: Exception => None
    }
}

object IntegerIsIntegral {

  implicit val integerIsIntegral = new IntegerIsIntegral {}
}
