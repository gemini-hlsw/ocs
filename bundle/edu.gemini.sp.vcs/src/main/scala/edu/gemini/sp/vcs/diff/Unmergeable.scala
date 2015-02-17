package edu.gemini.sp.vcs.diff

/** An error case used when a merge cannot be performed for some reason. */
case class Unmergeable(why: String)
