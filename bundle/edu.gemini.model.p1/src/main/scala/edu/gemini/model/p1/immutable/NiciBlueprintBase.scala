package edu.gemini.model.p1.immutable

trait NiciBlueprintBase extends GeminiBlueprintBase {
  def instrument = Instrument.Nici
  def dichroic: NiciDichroic
  def redFilters: List[NiciRedFilter]
  def blueFilters: List[NiciBlueFilter]

  private def formatFilters(color: String, filters: List[String]): String =
    if (filters.size == 0) "" else filters.mkString(color +"(", "+", ")")

  protected def formatFilters: String =
    "%s%s".format(formatFilters(" Red", redFilters.map(_.value)),
                  formatFilters(" Blue", blueFilters.map(_.value)))
}