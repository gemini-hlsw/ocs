package edu.gemini.spModel.target

sealed trait GuideStarStatus
case object  ManualGuideStar    extends GuideStarStatus
case object  AutomaticGuideStar extends GuideStarStatus
