package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object BlueprintBase {
  def apply(a: M.BlueprintBase): BlueprintBase = a match {
    // Alopeke
    case b: M.AlopekeBlueprint              => AlopekeBlueprint(b)

    // Flamingos2
    case b: M.Flamingos2BlueprintImaging    => Flamingos2BlueprintImaging(b)
    case b: M.Flamingos2BlueprintLongslit   => Flamingos2BlueprintLongslit(b)
    case b: M.Flamingos2BlueprintMos        => Flamingos2BlueprintMos(b)

    // GMOS-S
    case b: M.GmosSBlueprintIfu             => GmosSBlueprintIfu(b)
    case b: M.GmosSBlueprintIfuNs           => GmosSBlueprintIfuNs(b)
    case b: M.GmosSBlueprintImaging         => GmosSBlueprintImaging(b)
    case b: M.GmosSBlueprintLongslit        => GmosSBlueprintLongslit(b)
    case b: M.GmosSBlueprintLongslitNs      => GmosSBlueprintLongslitNs(b)
    case b: M.GmosSBlueprintMos             => GmosSBlueprintMos(b)

    // GMOS-N
    case b: M.GmosNBlueprintIfu             => GmosNBlueprintIfu(b)
    case b: M.GmosNBlueprintImaging         => GmosNBlueprintImaging(b)
    case b: M.GmosNBlueprintLongslit        => GmosNBlueprintLongslit(b)
    case b: M.GmosNBlueprintLongslitNs      => GmosNBlueprintLongslitNs(b)
    case b: M.GmosNBlueprintMos             => GmosNBlueprintMos(b)

    // GNIRS
    case b: M.GnirsBlueprintImaging         => GnirsBlueprintImaging(b)
    case b: M.GnirsBlueprintSpectroscopy    => GnirsBlueprintSpectroscopy(b)

    // GSOAI
    case b: M.GsaoiBlueprint                => GsaoiBlueprint(b)

    // GRACES
    case b: M.GracesBlueprint               => GracesBlueprint(b)

    // GPI
    case b: M.GpiBlueprint                  => GpiBlueprint(b)

    // IGRINS
    case b: M.IgrinsBlueprint               => IgrinsBlueprint(b)

    // Michelle
    case b: M.MichelleBlueprintImaging      => MichelleBlueprintImaging(b)
    case b: M.MichelleBlueprintSpectroscopy => MichelleBlueprintSpectroscopy(b)

    // NICI
    case b: M.NiciBlueprintCoronagraphic    => NiciBlueprintCoronagraphic(b)
    case b: M.NiciBlueprintStandard         => NiciBlueprintStandard(b)

    // NIFS
    case b: M.NifsBlueprint                 => NifsBlueprint(b)
    case b: M.NifsBlueprintAo               => NifsBlueprintAo(b)

    // NIRI
    case b: M.NiriBlueprint                 => NiriBlueprint(b)

    // Phoenix
    case b: M.PhoenixBlueprint              => PhoenixBlueprint(b)

    // Speckle
    case b: M.DssiBlueprint                 => DssiBlueprint(b)

    // Texes
    case b: M.TexesBlueprint                => TexesBlueprint(b)

    // T-ReCS
    case b: M.TrecsBlueprintImaging         => TrecsBlueprintImaging(b)
    case b: M.TrecsBlueprintSpectroscopy    => TrecsBlueprintSpectroscopy(b)

    // Zorro
    case b: M.ZorroBlueprint                => ZorroBlueprint(b)

    // Exchange
    case b: M.SubaruBlueprint               => SubaruBlueprint(b)
    case b: M.KeckBlueprint                 => KeckBlueprint(b)

    // MaroonX
    case b: M.MaroonXBlueprint              => MaroonXBlueprint(b)

    case b: M.VisitorBlueprint              => VisitorBlueprint(b)
  }
}

trait BlueprintBase {
  def name: String
  def site: Site
  def toChoice(n:Namer): Object // TODO: this will go away
  def mutable(n:Namer):M.BlueprintBase
  val visitor: Boolean =  false
}

trait GeminiBlueprintBase extends BlueprintBase {
  def instrument: Instrument
  def site = instrument.site
  def ao: AoPerspective = AoNone
  def isVisitor(m:M.BlueprintBase) = m.isVisitor
}
