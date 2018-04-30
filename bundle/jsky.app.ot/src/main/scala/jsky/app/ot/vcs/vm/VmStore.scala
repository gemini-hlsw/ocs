package jsky.app.ot.vcs.vm

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.{NodeVersions, VersionMap, EmptyNodeVersions, EmptyVersionMap}
import edu.gemini.spModel.core.SPProgramID

import scala.collection.mutable
import jsky.app.ot.shared.vcs.VersionMapFunctor.VmUpdate


/**
 * A global place to store a mapping from program IDs to the latest remote
 * VersionMap information.  I'm sorry.
 */
object VmStore extends mutable.Publisher[VmUpdateEvent] {
  type RemoteVersions = Map[SPProgramID, VersionMap]

  private var versionMaps = Map.empty[SPProgramID, VersionMap]

  def update(u: VmUpdate): Unit = update(u.pid, u.vm, force = false)

  def update(kv: (SPProgramID, VersionMap)): Unit = update(kv._1, kv._2, force = false)

  def update(pid: SPProgramID, newVm: VersionMap, force: Boolean): Unit = {
    val oldVm = versionMaps.getOrElse(pid, EmptyVersionMap)
    if (force || VersionMap.isNewer(newVm, oldVm)) {
      versionMaps = versionMaps + (pid -> newVm)
      publish(VmUpdateEvent(pid, Some(newVm)))
    }
  }

  def remove(pid: SPProgramID): Unit =
    if (versionMaps.contains(pid)) {
      versionMaps = versionMaps - pid
      publish(VmUpdateEvent(pid, Option.empty[VersionMap]))
    }

  def get(id: SPProgramID): Option[VersionMap] = versionMaps.get(id)

  def getOrEmpty(id: SPProgramID): VersionMap = get(id).getOrElse(EmptyVersionMap)

  def getOrNull(id: SPProgramID): VersionMap = get(id).orNull

  def nodeVersions(id: SPProgramID, key: SPNodeKey): Option[NodeVersions] =
    get(id).flatMap(_.get(key))

  def nodeVersionsOrEmpty(id: SPProgramID, key: SPNodeKey): NodeVersions =
    get(id).flatMap(_.get(key)).getOrElse(EmptyNodeVersions)

  def nodeVersionsOrNull(id: SPProgramID, key: SPNodeKey): NodeVersions =
    nodeVersions(id, key).orNull

  def all: RemoteVersions = versionMaps
}
