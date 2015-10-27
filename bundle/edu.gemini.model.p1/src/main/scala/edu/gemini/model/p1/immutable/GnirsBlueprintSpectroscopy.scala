package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object GnirsBlueprintSpectroscopy
  extends ((Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser, GnirsFpu, GnirsCentralWavelength) => GnirsBlueprintSpectroscopy) {

  def apply(m:M.GnirsBlueprintSpectroscopy):GnirsBlueprintSpectroscopy = new GnirsBlueprintSpectroscopy(m)

}

case class GnirsBlueprintSpectroscopy(altair:Altair, pixelScale:GnirsPixelScale,
                                      disperser:GnirsDisperser, crossDisperser:GnirsCrossDisperser, fpu:GnirsFpu, centralWavelength: GnirsCentralWavelength)
  extends GnirsBlueprintBase {

  def name = s"GNIRS Spectroscopy ${altair.shortName} ${pixelScale.value} ${disperser.value} ${crossDisperser.value} ${fpu.value} ${centralWavelength.value}"

  def this(m:M.GnirsBlueprintSpectroscopy) = this (
    Altair(m.getAltair),
    m.getPixelScale,
    m.getDisperser,
    m.getCrossDisperser,
    m.getFpu,
    m.getCentralWavelength
  )

  def mutable(n:Namer) = {
    val m = Factory.createGnirsBlueprintSpectroscopy
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setAltair(altair.mutable)
    m.setPixelScale(pixelScale)
    m.setDisperser(disperser)
    m.setCrossDisperser(crossDisperser)
    m.setFpu(fpu)
    m.setCentralWavelength(centralWavelength)
    m
  }

  def toChoice(n:Namer) = {
    val m = Factory.createGnirsBlueprintChoice()
    m.setSpectroscopy(mutable(n))
    m
  }

}