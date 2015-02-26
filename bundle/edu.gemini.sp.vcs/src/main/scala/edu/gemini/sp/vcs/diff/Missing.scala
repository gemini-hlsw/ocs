package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.NodeVersions

/** Marks a node that either never existed in the program or else has been
  * removed and had different version information. */
final case class Missing(key: SPNodeKey, nv: NodeVersions)
