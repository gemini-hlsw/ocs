package edu.gemini.pot.sp.version

import edu.gemini.pot.sp.SPNodeKey

/**
 * Basic operations required from Java but difficult or awkward without this
 * crutch.  From Scala, there's no need to look here.
 */
object JavaVersionMapOps {
  def emptyNodeVersions = EmptyNodeVersions
  def emptyVersionMap   = EmptyVersionMap

  def getOrNull(m: VersionMap, k: SPNodeKey): NodeVersions  = m.get(k).orNull
  def getOrEmpty(m: VersionMap, k: SPNodeKey): NodeVersions = m.get(k).getOrElse(EmptyNodeVersions)

  def isNewLocally(k: SPNodeKey, local: VersionMap, remote: VersionMap): Boolean =
    local.get(k).isDefined && remote.get(k).isEmpty

  def isModifiedLocally(k: SPNodeKey, local: VersionMap, remote: VersionMap): Boolean =
    (for {
      l <- local.get(k)
      r <- remote.get(k)
    } yield l.tryCompareTo(r).forall(_ > 0)).getOrElse(false)
}
