package edu.gemini.spModel.core

/**
 * A type class that combines conversion and construction for convenience.
 */
trait IsoAngle[A] extends ToDegrees[A] with FromDegrees[A] {

}
