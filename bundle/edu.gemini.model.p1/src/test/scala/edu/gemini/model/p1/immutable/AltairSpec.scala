package edu.gemini.model.p1.immutable

import org.specs2.mutable.Specification

class AltairSpec extends Specification {
  "Altair" should {
    "can be LGS and none of pwfs1/aowfs/oiwfs simultaneously" in {
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = false).toString must beEqualTo("Altair Laser Guidestar")
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = false).shortName must beEqualTo("LGS")
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = false) must not beNull
    }
    "can have one pwfs1 set" in {
      AltairLGS(pwfs1 = true, aowfs = false, oiwfs = false).toString must beEqualTo("Altair Laser Guidestar w/ PWFS1")
      AltairLGS(pwfs1 = true, aowfs = false, oiwfs = false).shortName must beEqualTo("LGS/PWFS1")
      AltairLGS(pwfs1 = true, aowfs = false, oiwfs = false) must not beNull
    }
    "can have one aowfs set" in {
      AltairLGS(pwfs1 = false, aowfs = true, oiwfs = false).toString must beEqualTo("Altair Laser Guidestar")
      AltairLGS(pwfs1 = false, aowfs = true, oiwfs = false).shortName must beEqualTo("LGS/AOWFS")
      AltairLGS(pwfs1 = false, aowfs = true, oiwfs = false) must not beNull
    }
    "can have one oiwfs set" in {
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = true).toString must beEqualTo("Altair Laser Guidestar w/ OIWFS")
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = true).shortName must beEqualTo("LGS/OIWFS")
      AltairLGS(pwfs1 = false, aowfs = false, oiwfs = true) must not beNull
    }
    "cannot have more than one pwfs1/aowfs/oiwfs simultaneously" in {
      AltairLGS(pwfs1 = true, aowfs = true, oiwfs = false) must throwA[IllegalArgumentException]
      AltairLGS(pwfs1 = false, aowfs = true, oiwfs = true) must throwA[IllegalArgumentException]
      AltairLGS(pwfs1 = true, aowfs = false, oiwfs = true) must throwA[IllegalArgumentException]
      AltairLGS(pwfs1 = true, aowfs = true, oiwfs = true) must throwA[IllegalArgumentException]
    }
  }
}
