package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._

import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.flamingos2.blueprint.{SpFlamingos2BlueprintMos, SpFlamingos2BlueprintLongslit, SpFlamingos2BlueprintImaging}
import edu.gemini.spModel.gemini.gmos.blueprint._
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, GmosNorthType, GmosSouthType}
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.michelle.blueprint.{SpMichelleBlueprintSpectroscopy, SpMichelleBlueprintImaging}
import edu.gemini.spModel.gemini.phoenix.PhoenixParams
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

import edu.gemini.spModel.template.SpBlueprint

import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.altair.blueprint.{SpAltairLgs, SpAltairNgs, SpAltairNone, SpAltair}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gnirs.blueprint.{SpGnirsBlueprintSpectroscopy, SpGnirsBlueprintImaging}
import edu.gemini.spModel.gemini.nici.NICIParams
import edu.gemini.spModel.gemini.nici.blueprint.{SpNiciBlueprintStandard, SpNiciBlueprintCoronagraphic}
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.nifs.blueprint.{SpNifsBlueprintAo, SpNifsBlueprint}
import edu.gemini.spModel.gemini.trecs.TReCSParams
import edu.gemini.spModel.gemini.trecs.blueprint.{SpTrecsBlueprintSpectroscopy, SpTrecsBlueprintImaging}
import edu.gemini.spModel.gemini.niri.blueprint.SpNiriBlueprint
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.gsaoi.blueprint.SpGsaoiBlueprint
import edu.gemini.spModel.gemini.texes.TexesParams
import edu.gemini.spModel.gemini.texes.blueprint.SpTexesBlueprint
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint
import edu.gemini.spModel.gemini.gpi.Gpi
import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint

object SpBlueprintFactory {

  def create(base: BlueprintBase): Either[String, SpBlueprint] =
    base match {
      case b: Flamingos2BlueprintImaging    => F2.imaging(b)
      case b: Flamingos2BlueprintLongslit   => F2.longslit(b)
      case b: Flamingos2BlueprintMos        => F2.mos(b)
      case b: GmosNBlueprintIfu             => GmosN.ifu(b)
      case b: GmosNBlueprintImaging         => GmosN.imaging(b)
      case b: GmosNBlueprintLongslit        => GmosN.longslit(b)
      case b: GmosNBlueprintLongslitNs      => GmosN.longslitNs(b)
      case b: GmosNBlueprintMos             => GmosN.mos(b)
      case b: GmosSBlueprintIfu             => GmosS.ifu(b)
      case b: GmosSBlueprintIfuNs           => GmosS.ifuNs(b)
      case b: GmosSBlueprintImaging         => GmosS.imaging(b)
      case b: GmosSBlueprintLongslit        => GmosS.longslit(b)
      case b: GmosSBlueprintLongslitNs      => GmosS.longslitNs(b)
      case b: GmosSBlueprintMos             => GmosS.mos(b)
      case b: GnirsBlueprintImaging         => Gnirs.imaging(b)
      case b: GnirsBlueprintSpectroscopy    => Gnirs.spectroscopy(b)
      case b: GpiBlueprint                  => GpiHandler(b)
      case b: GsaoiBlueprint                => GsaoiHandler(b)
      case b: MichelleBlueprintImaging      => Michelle.imaging(b)
      case b: MichelleBlueprintSpectroscopy => Michelle.spectroscopy(b)
      case b: NiciBlueprintCoronagraphic    => Nici.coronographic(b)
      case b: NiciBlueprintStandard         => Nici.standard(b)
      case b: NifsBlueprint                 => Nifs.nifs(b)
      case b: NifsBlueprintAo               => Nifs.ao(b)
      case b: TrecsBlueprintImaging         => Trecs.imaging(b)
      case b: TrecsBlueprintSpectroscopy    => Trecs.spectroscopy(b)
      case b: NiriBlueprint                 => NiriHandler(b)
      case b: TexesBlueprint                => TexesHandler(b)
      case b: PhoenixBlueprint              => PhoenixHandler(b)

      // Visitors blueprints
      case b: VisitorBlueprint              => VisitorHandler(b)
      case b: AlopekeBlueprint              => VisitorHandler(b)
      case b: DssiBlueprint                 => VisitorHandler(b)
      case b: ZorroBlueprint                => VisitorHandler(b)
      case b: GracesBlueprint               => Graces(b)
      case _                                => Left("Unexpected blueprint: " + base)
    }

  private def spEnum[E1 <: Enum[E1], E2 <: Enum[E2]](e1: E1, c2: Class[E2]): Either[String, E2] = {
    val e2Opt = c2.getEnumConstants find { _.name() == e1.name() }
    e2Opt.toRight("Could not find matching enum value for %s.%s".format(e1.getClass.getName, e1.name()))
  }

  private def spEnumList[E1 <: Enum[E1], E2 <: Enum[E2]](lst: Traversable[E1], c2: Class[E2]): Either[String, java.util.List[E2]] = {
    val empty: Either[String, List[E2]] = Right(Nil)
    val scalaSpEnumList = (lst :\ empty) {
      (e1, cur) =>
        cur.right flatMap { e2lst =>
          spEnum(e1, c2).right map { e2 => e2 :: e2lst }
        }
    }
    scalaSpEnumList.right map { _.asJava }
  }

  object ToSpAltair {
    private def fieldLens(fl: Boolean) =
      if (fl) AltairParams.FieldLens.IN else AltairParams.FieldLens.OUT

    def apply(a: Altair): SpAltair = a match {
      case AltairNone            => SpAltairNone.instance
      case AltairNGS(fl)         => new SpAltairNgs(fieldLens(fl))
      case AltairLGS(p1, ao, oi) =>
        if (p1) new SpAltairLgs(SpAltairLgs.LgsMode.LGS_P1)
        else if (oi) new SpAltairLgs(SpAltairLgs.LgsMode.LGS_OI)
        else new SpAltairLgs(SpAltairLgs.LgsMode.LGS)
    }
  }

  object Graces {
    private def readMode(rm: GracesReadMode)   = spEnum(rm, classOf[SpGracesBlueprint.ReadMode])
    private def fiberMode(fm: GracesFiberMode) = spEnum(fm, classOf[SpGracesBlueprint.FiberMode])

    def apply(b: GracesBlueprint): Either[String, SpGracesBlueprint] =
      for {
        r <- readMode(b.readMode).right
        f <- fiberMode(b.fiberMode).right
      } yield new SpGracesBlueprint(r, f)
  }

  object F2 {
    private def filters(lst: List[Flamingos2Filter]) = spEnumList(lst, classOf[Flamingos2.Filter])
    private def disperser(p1: Flamingos2Disperser)   = spEnum(p1, classOf[Flamingos2.Disperser])
    private def fpu(p1: Flamingos2Fpu)               = spEnum(p1, classOf[Flamingos2.FPUnit])

    def imaging(b: Flamingos2BlueprintImaging): Either[String, SpBlueprint] =
      for {
        fs <- filters(b.filters).right
      } yield new SpFlamingos2BlueprintImaging(fs)

    def longslit(b: Flamingos2BlueprintLongslit): Either[String, SpBlueprint] =
      for {
        d  <- disperser(b.disperser).right
        fs <- filters(b.filters).right
        u  <- fpu(b.fpu).right
      } yield new SpFlamingos2BlueprintLongslit(fs, d, u)

    def mos(b: Flamingos2BlueprintMos): Either[String, SpBlueprint] =
      for {
        d  <- disperser(b.disperser).right
        fs <- filters(b.filters).right
      } yield new SpFlamingos2BlueprintMos(fs, d, b.preImaging)
  }

  object GmosN {

    // USER_SUPPLIED maps to NONE
    import edu.gemini.model.p1.mutable.{ GmosNFilter => M }
    private def hackFilter(p:GmosNFilter) = p match {
      case M.USER_SUPPLIED => M.NONE
      case _ => p
    }

    private def disperser(p1: GmosNDisperser)   = spEnum(p1, classOf[GmosNorthType.DisperserNorth])
    private def filter(p1: GmosNFilter)         = spEnum(hackFilter(p1), classOf[GmosNorthType.FilterNorth])
    private def filters(lst: List[GmosNFilter]) = spEnumList(lst.map(hackFilter), classOf[GmosNorthType.FilterNorth])
    private def fpu(p1: GmosNFpu)               = spEnum(p1, classOf[GmosNorthType.FPUnitNorth])
    private def fpuMos(p1: GmosNMOSFpu)         = spEnum(p1, classOf[GmosNorthType.FPUnitNorth])
    private def fpuIfu(p1: GmosNFpuIfu)         = spEnum(p1, classOf[GmosNorthType.FPUnitNorth])
    private def fpuNs(p1: GmosNFpuNs)           = spEnum(p1, classOf[GmosNorthType.FPUnitNorth])

    def ifu(b: GmosNBlueprintIfu): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuIfu(b.fpu).right
      } yield new SpGmosNBlueprintIfu(ToSpAltair(b.altair), d, f, u)

    def imaging(b: GmosNBlueprintImaging): Either[String, SpBlueprint] =
      for {
        fs <- filters(b.filters).right
      } yield new SpGmosNBlueprintImaging(ToSpAltair(b.altair), fs)

    def longslit(b: GmosNBlueprintLongslit): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpu(b.fpu).right
      } yield new SpGmosNBlueprintLongslit(ToSpAltair(b.altair), d, f, u)

    def longslitNs(b: GmosNBlueprintLongslitNs): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuNs(b.fpu).right
      } yield new SpGmosNBlueprintLongslitNs(ToSpAltair(b.altair), d, f, u)

    def mos(b: GmosNBlueprintMos): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuMos(b.fpu).right
      } yield new SpGmosNBlueprintMos(
        ToSpAltair(b.altair),
        d,
        f,
        if (b.nodAndShuffle) GmosCommonType.UseNS.TRUE else GmosCommonType.UseNS.FALSE,
        b.preImaging,
        u
      )
  }

  object GmosS {

    // USER_SUPPLIED maps to NONE
    import edu.gemini.model.p1.mutable.{ GmosSFilter => M }
    private def hackFilter(p:GmosSFilter) = p match {
      case M.USER_SUPPLIED => M.NONE
      case _ => p
    }

    private def disperser(p1: GmosSDisperser)   = spEnum(p1, classOf[GmosSouthType.DisperserSouth])
    private def filter(p1: GmosSFilter)         = spEnum(hackFilter(p1), classOf[GmosSouthType.FilterSouth])
    private def filters(lst: List[GmosSFilter]) = spEnumList(lst.map(hackFilter), classOf[GmosSouthType.FilterSouth])
    private def fpu(p1: GmosSFpu)               = spEnum(p1, classOf[GmosSouthType.FPUnitSouth])
    private def fpuMos(p1: GmosSMOSFpu)         = spEnum(p1, classOf[GmosSouthType.FPUnitSouth])
    private def fpuIfu(p1: GmosSFpuIfu)         = spEnum(p1, classOf[GmosSouthType.FPUnitSouth])
    private def fpuIfuNs(p1: GmosSFpuIfuNs)     = spEnum(p1, classOf[GmosSouthType.FPUnitSouth])
    private def fpuNs(p1: GmosSFpuNs)           = spEnum(p1, classOf[GmosSouthType.FPUnitSouth])

    def ifu(b: GmosSBlueprintIfu): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuIfu(b.fpu).right
      } yield new SpGmosSBlueprintIfu(d, f, u)

    def ifuNs(b: GmosSBlueprintIfuNs): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuIfuNs(b.fpu).right
      } yield new SpGmosSBlueprintIfuNs(d, f, u)

    def imaging(b: GmosSBlueprintImaging): Either[String, SpBlueprint] =
      for {
        fs <- filters(b.filters).right
      } yield new SpGmosSBlueprintImaging(fs)

    def longslit(b: GmosSBlueprintLongslit): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpu(b.fpu).right
      } yield new SpGmosSBlueprintLongslit(d, f, u)

    def longslitNs(b: GmosSBlueprintLongslitNs): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuNs(b.fpu).right
      } yield new SpGmosSBlueprintLongslitNs(d, f, u)

    def mos(b: GmosSBlueprintMos): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        f <- filter(b.filter).right
        u <- fpuMos(b.fpu).right
      } yield new SpGmosSBlueprintMos(
        d,
        f,
        if (b.nodAndShuffle) GmosCommonType.UseNS.TRUE else GmosCommonType.UseNS.FALSE,
        b.preImaging,
        u
      )
  }

  object Michelle {
    private def filters(lst: List[MichelleFilter]) = spEnumList(lst, classOf[MichelleParams.Filter])
    private def fpu(p1: MichelleFpu)               = spEnum(p1, classOf[MichelleParams.Mask])
    private def disperser(p1: MichelleDisperser)   = spEnum(p1, classOf[MichelleParams.Disperser])

    def imaging(b: MichelleBlueprintImaging): Either[String, SpBlueprint] =
      for {
        fs <- filters(b.filters).right
      } yield new SpMichelleBlueprintImaging(fs, b.polarimetry == MichellePolarimetry.YES)

    def spectroscopy(b: MichelleBlueprintSpectroscopy): Either[String, SpBlueprint] =
      for {
        u <- fpu(b.fpu).right
        d <- disperser(b.disperser).right
      } yield new SpMichelleBlueprintSpectroscopy(u, d)
  }

  object Gnirs {
    private def pixelScale(p1: GnirsPixelScale)     = spEnum(p1, classOf[GNIRSParams.PixelScale])
    private def filter(p1: GnirsFilter)             = spEnum(p1, classOf[GNIRSParams.Filter])
    private def disperser(p1: GnirsDisperser)       = spEnum(p1, classOf[GNIRSParams.Disperser])
    private def xdisperser(p1: GnirsCrossDisperser) = spEnum(p1, classOf[GNIRSParams.CrossDispersed])
    private def fpu(p1: GnirsFpu)                   = spEnum(p1, classOf[GNIRSParams.SlitWidth])


    def imaging(b: GnirsBlueprintImaging): Either[String, SpBlueprint] =
      for {
        p <- pixelScale(b.pixelScale).right
        f <- filter(b.filter).right
      } yield new SpGnirsBlueprintImaging(ToSpAltair(b.altair), p, f)

    def spectroscopy(b: GnirsBlueprintSpectroscopy): Either[String, SpBlueprint] =
      for {
        p <- pixelScale(b.pixelScale).right
        d <- disperser(b.disperser).right
        x <- xdisperser(b.crossDisperser).right
        u <- fpu(b.fpu).right
      } yield new SpGnirsBlueprintSpectroscopy(ToSpAltair(b.altair), p, d, x, u, b.centralWavelength == edu.gemini.model.p1.mutable.GnirsCentralWavelength.GTE_25)
  }

  object Nici {
    private def fpm(p1: NiciFpm)                 = spEnum(p1, classOf[NICIParams.FocalPlaneMask])
    private def dichroic(p1: NiciDichroic)       = spEnum(p1, classOf[NICIParams.DichroicWheel])
    private def reds(lst: List[NiciRedFilter])   = spEnumList(lst, classOf[NICIParams.Channel1FW])
    private def blues(lst: List[NiciBlueFilter]) = spEnumList(lst, classOf[NICIParams.Channel2FW])

    def coronographic(b: NiciBlueprintCoronagraphic): Either[String, SpBlueprint] =
      for {
        m  <- fpm(b.fpm).right
        d  <- dichroic(b.dichroic).right
        rs <- reds(b.redFilters).right
        bs <- blues(b.blueFilters).right
      } yield new SpNiciBlueprintCoronagraphic(m, d, rs, bs)

    def standard(b: NiciBlueprintStandard): Either[String, SpBlueprint] =
      for {
        d  <- dichroic(b.dichroic).right
        rs <- reds(b.redFilters).right
        bs <- blues(b.blueFilters).right
      } yield new SpNiciBlueprintStandard(d, rs, bs)
  }

  object Nifs {
    private def disperser(p1: NifsDisperer)  = spEnum(p1, classOf[NIFSParams.Disperser])
    private def occultingDisk(p1: NifsOccultingDisk) = spEnum(p1, classOf[NIFSParams.Mask])

    def nifs(b: NifsBlueprint): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
      } yield new SpNifsBlueprint(d)

    def ao(b: NifsBlueprintAo): Either[String, SpBlueprint] =
      for {
        o <- occultingDisk(b.occultingDisk).right
        d <- disperser(b.disperser).right
      } yield new SpNifsBlueprintAo(ToSpAltair(b.altair), o, d)
  }

  object Trecs {
    private def filters(lst: List[TrecsFilter]) = spEnumList(lst, classOf[TReCSParams.Filter])
    private def disperser(p1: TrecsDisperser)   = spEnum(p1, classOf[TReCSParams.Disperser])
    private def fpu(p1: TrecsFpu)               = spEnum(p1, classOf[TReCSParams.Mask])

    def imaging(b: TrecsBlueprintImaging): Either[String, SpBlueprint] =
      for {
        fs <- filters(b.filters).right
      } yield new SpTrecsBlueprintImaging(fs)

    def spectroscopy(b: TrecsBlueprintSpectroscopy): Either[String, SpBlueprint] =
      for {
        d <- disperser(b.disperser).right
        u <- fpu(b.fpu).right
      } yield new SpTrecsBlueprintSpectroscopy(d, u)
  }

  object NiriHandler {

    private def camera(c:NiriCamera) = spEnum(c, classOf[Niri.Camera])
    private def filters(fs: List[NiriFilter]) = spEnumList(fs, classOf[Niri.Filter])

    def apply(b: NiriBlueprint):Either[String, SpNiriBlueprint] =
      for {
        c <- camera(b.camera).right
        f <- filters(b.filters).right
      } yield new SpNiriBlueprint(ToSpAltair(b.altair), c, f)

  }

  object GsaoiHandler {

    private def filters(fs: List[GsaoiFilter]) = spEnumList(fs, classOf[Gsaoi.Filter])

    def apply(b: GsaoiBlueprint):Either[String, SpGsaoiBlueprint] =
      for {
        f <- filters(b.filters).right
      } yield new SpGsaoiBlueprint(f)

  }

  object TexesHandler {

    private def disperser(c:TexesDisperser) = spEnum(c, classOf[TexesParams.Disperser])

    def apply(b: TexesBlueprint):Either[String, SpTexesBlueprint] =
      for {
        c <- disperser(b.disperser).right
      } yield new SpTexesBlueprint(c)
  }

  object VisitorHandler {
    def apply(b: VisitorBlueprint):Either[String, SpVisitorBlueprint] = Right(new SpVisitorBlueprint(b.customName))
    def apply(b: AlopekeBlueprint):Either[String, SpVisitorBlueprint] = Right(new SpVisitorBlueprint(b.name))
    def apply(b: ZorroBlueprint):Either[String, SpVisitorBlueprint] = Right(new SpVisitorBlueprint(b.name))
    def apply(b: DssiBlueprint):Either[String, SpVisitorBlueprint] = Right(new SpVisitorBlueprint("DSSI"))
  }

  object GpiHandler {

    private def disperser(c: GpiDisperser) = spEnum(c, classOf[Gpi.Disperser])
    private def observingMode(c: GpiObservingMode) = spEnum(c, classOf[Gpi.ObservingMode])

    def apply(b: GpiBlueprint):Either[String, SpGpiBlueprint] =
      for {
        d <- disperser(b.disperser).right
        o <- observingMode(b.observingMode).right
      } yield new SpGpiBlueprint(d, o)
  }

  object PhoenixHandler {

    private def fpu(m: PhoenixFocalPlaneUnit) = spEnum(m, classOf[PhoenixParams.Mask])
    private def filter(f: PhoenixFilter) = spEnum(f, classOf[PhoenixParams.Filter])

    def apply(b: PhoenixBlueprint): Either[String, SpPhoenixBlueprint] =
      for {
        m <- fpu(b.fpu).right
        f <- filter(b.filter).right
      } yield SpPhoenixBlueprint(m, f)

  }

}
