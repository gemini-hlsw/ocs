package edu.gemini.model.p1.immutable

trait GnirsBlueprintBase extends GeminiBlueprintBase {
  def altair: Altair
  def pixelScale: GnirsPixelScale
  def instrument = Instrument.Gnirs
  override def ao: AoPerspective = altair.ao
}

