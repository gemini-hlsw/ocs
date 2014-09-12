package edu.gemini.sp.vcs.log

sealed trait VcsOp
case object OpFetch extends VcsOp
case object OpStore extends VcsOp
