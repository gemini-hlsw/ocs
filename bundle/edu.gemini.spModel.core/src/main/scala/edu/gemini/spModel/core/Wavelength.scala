package edu.gemini.spModel.core

import scalaz.{Order, Monoid}

/** Representation of wavelengths. */
sealed trait Wavelength extends Serializable {

  /** The wavelength as nanometers.
    * @group Conversions
    */
  def toNanometers: Double

  /** The wavelength as microns or micrometers.
    * @group Conversions
    */
  def toMicrons: Double = toNanometers / 1000

  /**
   * Addition.
   * @group Operations
   */
  def +(a: Wavelength): Wavelength =
    Wavelength.fromNanometers(toNanometers + a.toNanometers)

  /**
   * Subtraction.
   * @group Operations
   */
  def -(a: Wavelength): Wavelength =
    Wavelength.fromNanometers(toNanometers - a.toNanometers)

  /**
   * Scalar multiplication.
   * @group Operations
   */
  def *(factor: Double): Wavelength =
    Wavelength.fromNanometers(toNanometers * factor)

  /**
   * Scalar division.
   * @group Operations
   */
  def /(factor: Double): Wavelength =
    Wavelength.fromNanometers(toNanometers / factor)

  /** @group Overrides */
  final override def toString =
    s"Wavelength(${toNanometers}nm)"

  /** @group Overrides */
  final override def equals(a: Any) =
    a match {
      case a: Wavelength => a.toNanometers == this.toNanometers
      case _             => false
    }

  /** @group Overrides */
  final override def hashCode =
    toNanometers.hashCode
}

object Wavelength {

  /**
   * Constructs a `Wavelength` from the given value in nanometers.
   * @group Constructors
   */
  def fromNanometers(l: Double) = new Wavelength {
    require(l >= 0)
    override val toNanometers = l
  }

  /**
   * Constructs a `Wavelength` from the given value in microns (aka. micrometers).
   * @group Constructors
   */
  def fromMicrons(l: Double) = new Wavelength {
    require(l >= 0)
    override val toNanometers = l * 1000
  }

  /**
   * The zero `Wavelength`.
   * @group Constructors
   */
  lazy val zero = fromNanometers(0.0)

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

