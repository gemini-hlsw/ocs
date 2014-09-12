package edu.gemini.model.p1.immutable

trait Flamingos2BlueprintSpectroscopyBase extends Flamingos2BlueprintBase {
  def disperser: Flamingos2Disperser

  def withFilters(lst: List[Flamingos2Filter]): Flamingos2BlueprintSpectroscopyBase
}