package edu.gemini.model.p1.targetio.table

/**
 * Provides support for working with the STIL library.
 */
abstract class StilSerializer[T] {
  def asBinary(value: T): Any
  def primitiveClass: Class[_]
  def asText(value: T): String
}