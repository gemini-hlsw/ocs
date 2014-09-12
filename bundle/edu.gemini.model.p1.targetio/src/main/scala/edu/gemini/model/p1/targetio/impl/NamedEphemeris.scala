package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.EphemerisElement

/**
 * Groups an {@link EphemerisElement} with a target name.
 */
case class NamedEphemeris(ord: Int, name: String, element: EphemerisElement)

