package edu.gemini.spModel.core

import org.scalatest.FlatSpec


class FormatTest extends FlatSpec {

  private def testHms(in: String, digits: Int, out: String): Unit = {
    assert(Angle.parseHMS(in).exists { a =>
      Angle.formatHMS(a, fractionalDigits = digits) == out
    })
  }

  "HMS second formatting" should "not carry over if under 60" in {
    testHms("0:00:59.9998", 4, "0:00:59.9998")
  }

  it should "carry over if rounding up to 60" in {
    testHms("0:00:59.9998", 3, "0:01:00.000")
  }

  it should "carry over to hours if rounding up minutes as well" in {
    testHms("0:59:59.9998", 1, "1:00:00.0")
  }

  it should "wrap around to 0 if rounding up hours to 24" in {
    testHms("23:59:59.9998", 2, "0:00:00.00")
  }

  "DMS second formatting" should "wrap around to 0 if rounding up degrees to 360" in {
    assert(Angle.parseDMS("359:59:59.9998").exists { a =>
      Angle.formatDMS(a) == "0:00:00.00"
    })
  }

  private def testDec(in: String, digits: Int, out: String): Unit = {
    val dec = for {
      a <- Angle.parseDMS(in).toOption
      d <- Declination.fromAngle(a)
    } yield d

    assert(dec.exists { d =>
      Declination.formatDMS(d, fractionalDigits = digits) == out
    })
  }

  "Declination second formatting" should "carry not carry over if under 60" in {
    testDec("-0:00:59.9998", 4, "-0:00:59.9998")
  }

  it should "carry over if rounding up to 60" in {
    testDec("-0:00:59.9998", 3, "-0:01:00.000")
  }

  it should "carry over to degrees if rounding up minutes as well" in {
    testDec("-0:59:59.9998", 2, "-1:00:00.00")
  }

  "HMS formatting" should "allow a custom separator" in {
    assert(Angle.parseHMS("1:02:03.456").exists { a =>
      Angle.formatHMS(a, " ", 1) == "1 02 03.5"
    })
  }
}
