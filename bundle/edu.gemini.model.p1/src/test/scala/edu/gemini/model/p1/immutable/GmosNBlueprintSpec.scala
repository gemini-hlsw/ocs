package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import org.specs2.mutable._

class GmosNBlueprintSpec extends Specification with SemesterProperties {

  "The GmosN Blueprint" should {
    "removes the 0.25 longslit, REL-1256" in {
      GmosNFpu.forName("0.25 arcsec slit") must throwA[IllegalArgumentException]
    }
    "support No Altair, REL-1671" in {
      val altair = new M.AltairChoice()
      altair.setNone(new M.AltairNone)
      val bp = GmosNBlueprintIfu(Altair(altair), GmosNDisperser.forName("B1200_G5301"), GmosNFilter.forName("GG455_G0305"), GmosNFpuIfu.forName("IFU_1"))
      bp.altair must be(AltairNone)
      bp.altair.ao must be(AoNone)
    }
    "support Altair NGS, REL-1671" in {
      val altair = new M.AltairChoice()
      val ngs = new M.AltairNGS()
      ngs.setFieldLens(false)
      altair.setNgs(ngs)
      val bp = GmosNBlueprintIfu(Altair(altair), GmosNDisperser.forName("B1200_G5301"), GmosNFilter.forName("GG455_G0305"), GmosNFpuIfu.forName("IFU_1"))
      bp.altair must beEqualTo(AltairNGS(fieldLens = false))
      bp.altair.ao must be(AoNgs)
    }
    "support Altair LGS, with aowfs, REL-1671" in {
      val altair = new M.AltairChoice()
      val ngs = new M.AltairLGS()
      ngs.setAowfs(true)
      altair.setLgs(ngs)
      val bp = GmosNBlueprintIfu(Altair(altair), GmosNDisperser.forName("B1200_G5301"), GmosNFilter.forName("GG455_G0305"), GmosNFpuIfu.forName("IFU_1"))
      bp.altair must beEqualTo(AltairLGS(pwfs1 = false, aowfs = true, oiwfs = false))
      bp.altair.ao must be(AoLgs)
    }
    "support Altair LGS, with pwfs1, REL-1671" in {
      val altair = new M.AltairChoice()
      val ngs = new M.AltairLGS()
      ngs.setPwfs1(true)
      altair.setLgs(ngs)
      val bp = GmosNBlueprintIfu(Altair(altair), GmosNDisperser.forName("B1200_G5301"), GmosNFilter.forName("GG455_G0305"), GmosNFpuIfu.forName("IFU_1"))
      bp.altair must beEqualTo(AltairLGS(pwfs1 = true, aowfs = false, oiwfs = false))
      bp.altair.ao must be(AoLgs)
    }
    "support Altair LGS, with oiwfs, REL-1671" in {
      val altair = new M.AltairChoice()
      val ngs = new M.AltairLGS()
      ngs.setOiwfs(true)
      altair.setLgs(ngs)
      val bp = GmosNBlueprintIfu(Altair(altair), GmosNDisperser.forName("B1200_G5301"), GmosNFilter.forName("GG455_G0305"), GmosNFpuIfu.forName("IFU_1"))
      bp.altair must beEqualTo(AltairLGS(pwfs1 = false, aowfs = false, oiwfs = true))
      bp.altair.ao must be(AoLgs)
    }
  }

}