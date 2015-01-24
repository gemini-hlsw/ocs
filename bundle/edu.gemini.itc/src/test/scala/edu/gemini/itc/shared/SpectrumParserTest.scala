package edu.gemini.itc.shared

import edu.gemini.itc.shared.SpectrumParser.{WLSpectrumParser, PlainSpectrumParser}
import org.junit.Test

/**
 * Tests for the spectrum file parser.
 */
class SpectrumParserTest {

  @Test
  def wlParseStringSimple(): Unit = {
    val r = new WLSpectrumParser().
      parseString(
        """#comment comment
          |
          |234
          |
          |320	0.00
          |
          |340	0.00
          |400	0.83 #adfasd
          |9 10
          |0.0 3.4
          |""".stripMargin)
    assert(r.successful)    // we are expecting success
    assert(r.get._1 == 234) // we are expecting effective wavelength 234
    assert(r.get._2.size == 5) // we are expecting 5 pairs
  }

  @Test
  def plainParseStringSimple(): Unit = {
    val r = new PlainSpectrumParser().
      parseString(
        """#comment comment
          |
          |320	0.00
          |
          |340	0.00
          |400	0.83 #adfasd
          |420	0.88
          |620	0.00
          |640	0.00
          |9 10
          |0.0 3.4
          |#comment""".stripMargin)
    assert(r.successful)    // we are expecting success
    assert(r.get.size == 8) // we are expecting 8 pairs
  }

  @Test
  def plainParseString(): Unit = {
    val r = new PlainSpectrumParser().
      parseString(
        """|# transmission of acq cam B filter
          |# columns are (1) wavelength in nm, (2) transmission.
          |# data are sampled every 20nm from scanned transmission curve
          |# 320nm data point added as 0.00 for extrapolation;
          |# data artificially set to 0.00 beyond 600nm in expectation of
          |# a new B filter
          |# author: Phil Puxley
          |# created: 20 Jan 2001
          |
          |320	0.00
          |340	0.00
          |360	0.03
          |380	0.54
          |400	0.83
          |420	0.88
          |440	0.84
          |460	0.75
          |480	0.49
          |500	0.23
          |520	0.06
          |540	0.02
          |560	0.04
          |580	0.01
          |600	0.00
          |620	0.00
          |640	0.00
          |660	0.00
          |3 4
          |5 6
          |7 8
          |9 10
          |0.0 3.4
          |
          |
        """.stripMargin)

    assert(r.successful)    // we are expecting success
    assert(r.get.size == 23) // we are expecting 23 pairs
  }

  //======
  @Test
  def parseString(): Unit = {
    val r = new PlainSpectrumParser().
      parseString(
        """|# transmission of acq cam B filter
          |# columns are (1) wavelength in nm, (2) transmission.
          |# data are sampled every 20nm from scanned transmission curve
          |# 320nm data point added as 0.00 for extrapolation;
          |# data artificially set to 0.00 beyond 600nm in expectation of
          |# a new B filter
          |# author: Phil Puxley
          |# created: 20 Jan 2001
          |
          |320	0.00
          |340	0.00
          |360	0.03
          |380	0.54
          |400	0.83
          |
          |
        """.stripMargin)

    assert(r.successful)    // we are expecting success
    assert(r.get.size == 5) // we are expecting 5 pairs
  }

  @Test
  def canParseWithComments(): Unit = {
    val r = new PlainSpectrumParser().
      parseString(
        """#comment
          |
          |
          |1,2
          |
          |
          |#more comment
          |
          |0.0 3.4
          |
          |#end with comment
          |""".stripMargin)

    assert(r.successful)
    assert(r.get.size == 2)
  }

  @Test
  def canParseWithCrap(): Unit = {
    val r = new PlainSpectrumParser().
      parseString("#comment\n\n \t \n\n\n \t   4 \t   5\n  \n\n   \n")
    assert(r.successful)
    assert(r.get.size == 1)
  }

  @Test
  def canParseWithoutNLatEnd(): Unit = {
    val r = new PlainSpectrumParser().
      parseString(
        """1,2
          |0.0 3.4""".stripMargin)

    assert(r.successful)
    assert(r.get.size == 2)
  }

  @Test
  def parseFile1(): Unit = {
    val r = new PlainSpectrumParser().parseFile("/acqcam/colfilt_B.dat")
    assert(r.successful)
    assert(r.get.size == 35)
  }

  @Test
  def parseFile2(): Unit = {
    val r = new PlainSpectrumParser().parseFile("/flamingos2/HK_G0806.dat")
    assert(r.successful)
    assert(r.get.size == 2900)
  }

  @Test
  def parseFile3(): Unit = {
    val r = new PlainSpectrumParser().parseFile("/gems/canopus_background.dat")
    assert(r.successful)
    assert(r.get.size == 4201)
  }


  @Test
  def parseFile4(): Unit = {
    val r = new WLSpectrumParser().parseFile("/michelle/michelle_Q.dat")
    assert(r.successful)
    assert(r.get._1 == 20789)
    assert(r.get._2.size == 278)
  }

  @Test
  def parseFile5(): Unit = {
    val r = new PlainSpectrumParser().parseFile("/nifs/nifs_ifu_trans.dat")
    assert(r.successful)
    assert(r.get.size == 13)
  }

  @Test
  def parseFile6(): Unit = {
    val r = new PlainSpectrumParser().parseFile("/gnirs/gnirs_G32.dat")
    assert(r.successful)
    assert(r.get.size == 160)
  }
}
