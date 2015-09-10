package edu.gemini.spModel.core

import squants.space.{Length, Microns, Nanometers}

import scala.util.Try
import scalaz.{Monoid, Order}

/** Representation of wavelengths.
  * Wavelength is internally represented by `squants` values of type `Length`. */
final case class Wavelength private(length: Length) extends AnyVal with Serializable {

  /** The wavelength as nanometers.
    * @group Conversions
    */
  def toNanometers: Double = length.toNanometers

  /** The wavelength as microns or micrometers.
    * @group Conversions
    */
  def toMicrons: Double = length.toMicrons

  /**
   * Addition.
   * @group Operations
   */
  def +(a: Wavelength): Wavelength = Wavelength(length + a.length)

  /**
   * Subtraction.
   * @group Operations
   */
  def -(a: Wavelength): Wavelength = Wavelength(length - a.length)

  /**
   * Scalar multiplication.
   * @group Operations
   */
  def *(factor: Double): Wavelength = Wavelength(length * factor)

  /**
   * Scalar division.
   * @group Operations
   */
  def /(factor: Double): Wavelength = Wavelength(length / factor)

  /**
   * toString()
   */
  override def toString: String = length.toString()

}

object Wavelength {

  /**
   * Creates a `Wavelength` from a string, e.g. "2.2 µm".
   * @group Constructors
   */
  def apply(s: String): Try[Wavelength] = for {length <- Length(s)} yield Wavelength(length)

  /**
   * Creates a `Wavelength` from numeric value representing nanometers.
   * @group Constructors
   */
  def fromNanometers[A](value: A)(implicit num : Numeric[A]) = Wavelength(Nanometers(value))

  /**
   * Creates a `Wavelength` from numeric value representing microns.
   * @group Constructors
   */
  def fromMicrons[A](value: A)(implicit num : Numeric[A]) = Wavelength(Microns(value))

  /**
   * The zero `Wavelength`.
   * @group Constructors
   */
  lazy val zero = Wavelength(Nanometers(0))

  /**
   * Additive monoid for `Wavelength`.
   * @group Typeclass Instances
   */
  implicit val WavelengthMonoid: Monoid[Wavelength] =
    new Monoid[Wavelength] {
      val zero = Wavelength.zero
      def append(a: Wavelength, b: => Wavelength): Wavelength = a + b
    }

  /** @group Typeclass Instances */
  implicit val WavelengthOrder: Order[Wavelength] =
    Order.orderBy(_.toNanometers)

  /** @group Typeclass Instances */
  implicit val WavelengthOrdering: scala.Ordering[Wavelength] =
    scala.Ordering.by(_.toNanometers)

}

object WavelengthConversions {

  // implicit conversion from numeric values to `Wavelength`
  implicit class wavelengthFromDouble[A](value: A)(implicit num : Numeric[A]) {
    def nm         = Wavelength.fromNanometers(value)
    def nanometers = Wavelength.fromNanometers(value)
    def microns    = Wavelength.fromMicrons(value)
  }

}


