package edu.gemini.spModel.pio.codec

sealed trait PioError
case class MissingKey(name: String) extends PioError
case class NullValue(name: String) extends PioError
case class ParseError(name: String, value: String, dataType: String) extends PioError
case class UnknownTag(tag: String, dataType: String) extends PioError
case class GeneralError(dataType: String) extends PioError

