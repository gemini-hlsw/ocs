package edu.gemini.model.p1.immutable

trait Flamingos2BlueprintBase extends GeminiBlueprintBase {
  def instrument = Instrument.Flamingos2
  def filters: List[Flamingos2Filter]
}