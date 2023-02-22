package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object GnirsBlueprintImaging extends ((Altair, GnirsPixelScale, GnirsFilter) => GnirsBlueprintImaging)  {

  def apply(m:M.GnirsBlueprintImaging):GnirsBlueprintImaging = new GnirsBlueprintImaging(m)

}

case class GnirsBlueprintImaging(altair:Altair, pixelScale:GnirsPixelScale, filter:GnirsFilter)
  extends GnirsBlueprintBase {

  def name:String = "GNIRS Imaging %s %s %s".format(
    altair, pixelScale.value, filter.value)

  def this(m:M.GnirsBlueprintImaging) = this (
    Altair(m.getAltair),
    m.getPixelScale,
    m.getFilter
  )

  def mutable(n:Namer) = {
    val m = Factory.createGnirsBlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setAltair(altair.mutable)
    m.setPixelScale(pixelScale)
    m.setFilter(filter)
    m
  }

  def toChoice(n:Namer) = {
    val m = Factory.createGnirsBlueprintChoice()
    m.setImaging(mutable(n))
    m
  }

}
