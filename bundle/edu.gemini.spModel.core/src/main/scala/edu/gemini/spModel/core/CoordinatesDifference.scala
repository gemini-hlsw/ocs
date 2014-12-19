package edu.gemini.spModel.core

case class CoordinatesDifference(posAngleDec: Angle, distanceArcsec: Angle)

/**
 * A class that represents the difference between two sky coordinates, a base
 * position and a second position.  The difference includes the angular
 * separation between the two coordinates (roughly speaking, the "distance"
 * between them), the position angle formed in degrees east of north
 * <p/>
 * <p>(Based on the Java version which is based on C version from A. P. Martinez)
 */
object CoordinatesDifference {
  def difference(base: Coordinates, point: Coordinates): CoordinatesDifference = {
    val radian: Double = 180.0 / Math.PI

    // coo transformed to radians
    val alf = point.ra.toAngle.toRadians
    val alf0 = base.ra.toAngle.toRadians
    val del = point.dec.toAngle.toRadians
    val del0 = base.dec.toAngle.toRadians

    val sd0 = Math.sin(del0)
    val sd = Math.sin(del)
    val cd0 = Math.cos(del0)
    val cd = Math.cos(del)
    val cosda = Math.cos(alf - alf0)
    val cosd = sd0 * sd + cd0 * cd * cosda
    val dist = if (!Math.acos(cosd).isNaN) Math.acos(cosd) else 0

    val phi = if (dist > 0.0000004) {
        val sind = Math.sin(dist)
        val pcospa = (sd * cd0 - cd * sd0 * cosda) / sind
        val cospa = if (Math.abs(pcospa) > 1.0) {
            pcospa  / Math.abs(pcospa)
          } else {
            pcospa
          }
        val sinpa = cd * Math.sin(alf - alf0) / sind
        val pphi = Math.acos(cospa)

        if (sinpa < 0.0) {
          (Math.PI * 2) - pphi
        } else {
          pphi
        }
      } else {
        0
      }

    CoordinatesDifference(Angle.fromDegrees(phi * radian), Angle.fromDegrees(60.0 * 60*dist * radian))
  }
}
