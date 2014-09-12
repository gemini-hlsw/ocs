package edu.gemini.model.p1.immutable

trait NifsBlueprintBase extends GeminiBlueprintBase {
  def disperser: NifsDisperer
  def instrument = Instrument.Nifs
}