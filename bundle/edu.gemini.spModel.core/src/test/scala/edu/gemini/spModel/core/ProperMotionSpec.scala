package edu.gemini.spModel.core

import org.scalacheck.Prop._
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import scala.math._
import AlmostEqual.AlmostEqualOps
import AngleSyntax._
import java.time.Instant

import scalaz._
import Scalaz._

class ProperMotionSpec extends Specification {
  "ProperMotion" should {
    "coordinatesOn corrected by cos(dec) case 1" in {
      val ra = Angle.fromHMS(11, 5, 28.577).map(RightAscension.fromAngle).get
      val dec = Angle.fromDMS(43, 31, 36.39).flatMap(Declination.fromAngle).get
      val coord = Coordinates(ra, dec)
      val pmra =
        RightAscensionAngularVelocity(AngularVelocity(-4406.469))
      val pmdec =
        DeclinationAngularVelocity(AngularVelocity(938.527))
      val pm = ProperMotion(pmra, pmdec)

      val target = SiderealTarget(
        "tgt",
        coord,
        Some(pm),
        None,
        Some(Parallax.unsafeFromMas(203877)),
        Nil,
        None,
        None
      )
      val refEpoch = Instant.ofEpochSecond(4102444800L)
      val posAt = pm.calculateAt(target, refEpoch)
      val ra1 =
        Angle.fromHMS(11, 4, 48.043284).map(RightAscension.fromAngle).get
      val dec1 =
        Angle.fromDMS(43, 33, 09.795210).flatMap(Declination.fromAngle).get
      posAt.ra.toAngle.toSignedDegrees ~= ra1.toAngle.toSignedDegrees
      posAt.dec.toAngle.toSignedDegrees ~= dec1.toAngle.toSignedDegrees
    }

    "coordinatesOn corrected by cos(dec) case 2" in {
      val ra = Angle.fromHMS(14, 29, 42.946).map(RightAscension.fromAngle).get
      val dec = Angle
        .parseDMS("-62:40:46.16")
        .toOption
        .flatMap(Declination.fromAngle)
        .get
      val coord = Coordinates(ra, dec)
      val pmra =
        RightAscensionAngularVelocity(AngularVelocity(-3781.741))
      val pmdec =
        DeclinationAngularVelocity(AngularVelocity(769.465))
      val pm = ProperMotion(pmra, pmdec)

      val target = SiderealTarget(
        "tgt",
        coord,
        Some(pm),
        None,
        Some(Parallax.unsafeFromMas(768465)),
        Nil,
        None,
        None
      )
      val refEpoch = Instant.ofEpochSecond(4102444800L)
      val posAt = pm.calculateAt(target, refEpoch)
      val ra1 =
        Angle.fromHMS(14, 28, 48.054809).map(RightAscension.fromAngle).get
      val dec1 =
        Angle
          .parseDMS("-62:39:28.54303")
          .toOption
          .flatMap(Declination.fromAngle)
          .get
      posAt.ra.toAngle.toSignedDegrees ~= ra1.toAngle.toSignedDegrees
      posAt.dec.toAngle.toSignedDegrees ~= dec1.toAngle.toSignedDegrees
    }
  }
}
