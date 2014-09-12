package edu.gemini.model.p1.immutable

trait GmosNBlueprintSpectrosopyBase extends GmosNBlueprintBase {
  def disperser: GmosNDisperser
  def filter: GmosNFilter
}

