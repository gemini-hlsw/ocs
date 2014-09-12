package edu.gemini.model.p1.immutable

trait GmosNBlueprintBase extends GeminiBlueprintBase {
  val instrument = Instrument.GmosNorth
  val altair: Altair

  override def ao: AoPerspective = altair.ao
}