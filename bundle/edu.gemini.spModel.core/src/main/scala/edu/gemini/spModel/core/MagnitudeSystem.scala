package edu.gemini.spModel.core

sealed trait MagnitudeSystem {
  def name: String
}

object MagnitudeSystem {
  
  case object VEGA {
    def name = "Vega"
  }
  
  case object AB {
    def name = "AB"
  }
  
  case object JY {
    def name = "Jy"
  }

}