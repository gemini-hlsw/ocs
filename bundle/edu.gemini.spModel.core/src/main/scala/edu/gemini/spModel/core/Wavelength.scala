package edu.gemini.spModel.core

/** Representation of wavelengths. */
sealed trait Wavelength extends Serializable {

  /** The wavelength as nanometers.
    * @group Conversions
    */
  def toNanometers: Double

  /** The wavelength as microns or micrometers.
    * @group Conversions
    */
  def toMicrons: Double = toNanometers * 1000

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

  def fromNanometers(l: Double) = new Wavelength {
    override val toNanometers = l
  }

  def fromMicrons(l: Double) = fromMicrometers(l)

  def fromMicrometers(l: Double) = new Wavelength {
    override val toNanometers = l * 1000
  }

}

