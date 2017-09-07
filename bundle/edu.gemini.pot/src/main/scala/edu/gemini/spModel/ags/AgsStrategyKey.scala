package edu.gemini.spModel.ags

import java.util.Comparator

/**
 *
 */
sealed trait AgsStrategyKey {
  def id: String
  def displayName: String = id.replaceAll("_", " ")
}

object AgsStrategyKey {
  case object AltairAowfsKey extends AgsStrategyKey {
    val id = "ALTAIR"
    override def displayName = "Altair"
  }

  case object Flamingos2OiwfsKey extends AgsStrategyKey {
    val id = "F2_OIWFS"
  }

  case object GemsKey extends AgsStrategyKey {
    val id = "GEMS" // TODO: was GemsAgs in GemsAgsStrategy. Does this matter?
    override def displayName = "GeMS AGS"
  }

  case object GmosNorthOiwfsKey extends AgsStrategyKey {
    val id = "GMOS-N_OIWFS"
  }

  case object GmosSouthOiwfsKey extends AgsStrategyKey {
    val id = "GMOS-S_OIWFS"
  }

  case object GnirsOiwfsKey extends AgsStrategyKey {
    val id = "GNIRS_OIWFS"
  }

  case object GpiOiwfsKey extends AgsStrategyKey {
    val id = "GPI_OIWFS"
  }

  case object NiciOiwfsKey extends AgsStrategyKey {
    val id = "NICI_OIWFS"
  }

  case object NifsOiwfsKey extends AgsStrategyKey {
    val id = "NIFS_OIWFS"
  }

  case object NiriOiwfsKey extends AgsStrategyKey {
    val id = "NIRI_OIWFS"
  }

  case object Pwfs1NorthKey extends AgsStrategyKey {
    val id = "GN_PWFS1"
  }

  case object Pwfs2NorthKey extends AgsStrategyKey {
    val id = "GN_PWFS2"
  }

  case object Pwfs1SouthKey extends AgsStrategyKey {
    val id = "GS_PWFS1"
  }

  case object Pwfs2SouthKey extends AgsStrategyKey {
    val id = "GS_PWFS2"
  }

  case object OffKey extends AgsStrategyKey {
    val id = "Off"
  }

  val All = List(
    AltairAowfsKey,
    Flamingos2OiwfsKey,
    GemsKey,
    GmosNorthOiwfsKey,
    GmosSouthOiwfsKey,
    GnirsOiwfsKey,
    NiciOiwfsKey,
    NifsOiwfsKey,
    NiriOiwfsKey,
    Pwfs1NorthKey,
    Pwfs2NorthKey,
    Pwfs1SouthKey,
    Pwfs2SouthKey,
    OffKey
  )

  val AllMap = All.map(k => k.id -> k).toMap

  def fromString(s: String): Option[AgsStrategyKey] = AllMap.get(s)

  def fromStringOrNull(s: String): AgsStrategyKey = fromString(s).orNull

  // For use from Java.
  object DisplayNameComparator extends Comparator[AgsStrategyKey] {
    override def compare(k1: AgsStrategyKey, k2: AgsStrategyKey): Int =
      k1.displayName.compareTo(k2.displayName)
  }

}

