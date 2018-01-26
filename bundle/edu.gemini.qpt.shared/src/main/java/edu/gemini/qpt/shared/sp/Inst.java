package edu.gemini.qpt.shared.sp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.PreImagingType;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.texes.TexesParams;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

import java.util.*;

@SuppressWarnings("unchecked")
public enum Inst {

    // Note: alphabetical order: REL-293

    ACQUISITION_CAMERA(InstAcqCam.SP_TYPE, true, true, true),

    // REL-293: Combine Altair component and AOWFS to one item with NGS and LGS children
    // (AOWFS child is needed as well, but is hidden)
    ALTAIR(InstAltair.SP_TYPE, true, false, true,
            AltairParams.GuideStarType.values(),
            arrayOf(AltairParams.GuideStarType.NGS),
            arrayOf(AltairAowfsGuider.instance)),

    // Not currently in use
    BHROS(InstBHROS.SP_TYPE, false, false, false),

    CANOPUS(SPComponentType.QPT_CANOPUS, // new SPInstComponentType(SPInstComponentType.INST_BROAD_TYPE, "Canopus", "Canopus"),
            false, true, true, CanopusWfs.values(), CanopusWfs.values()),

    FLAMINGOS2(Flamingos2.SP_TYPE, false, true, false,
            join(Flamingos2.FPUnit.values(), Flamingos2.Filter.values(), new Enum<?>[]{Flamingos2OiwfsGuideProbe.instance}),
            join(arrayOf(Flamingos2.FPUnit.FPU_NONE, Flamingos2.FPUnit.CUSTOM_MASK), Flamingos2.Filter.values(), new Enum<?>[]{Flamingos2OiwfsGuideProbe.instance}),
            join(Flamingos2.Disperser.values(), PreImagingType.values())),

    GMOS_NORTH(InstGmosNorth.SP_TYPE, true, false, true,
            join(FPUnitNorth.values(),
                    DisperserNorth.values(),
                    FilterNorth.values(),
                    GmosCommonType.DetectorManufacturer.values(),
                    new Enum<?>[]{GmosOiwfsGuideProbe.instance}),
            join(new Enum<?>[]{FPUnitNorth.FPU_NONE, FPUnitNorth.CUSTOM_MASK, DisperserNorth.MIRROR},
                    FilterNorth.values(),
                    new Enum<?>[]{GmosCommonType.DetectorManufacturer.E2V},
                    new Enum<?>[]{GmosOiwfsGuideProbe.instance}),
            join(GmosCommonType.UseNS.values(), PreImagingType.values())),

    GMOS_SOUTH(InstGmosSouth.SP_TYPE, false, true, true,
            join(FPUnitSouth.values(),
                    DisperserSouth.values(),
                    FilterSouth.values(),
                    GmosCommonType.DetectorManufacturer.values(),
                    new Enum<?>[]{GmosOiwfsGuideProbe.instance}),
            join(new Enum<?>[] { FPUnitSouth.FPU_NONE, FPUnitSouth.CUSTOM_MASK, DisperserSouth.MIRROR },
                    FilterSouth.values(),
                    new Enum<?>[]{GmosCommonType.DetectorManufacturer.HAMAMATSU},
                    new Enum<?>[]{GmosOiwfsGuideProbe.instance}),
             join(GmosCommonType.UseNS.values(), PreImagingType.values())),


    GNIRS(InstGNIRS.SP_TYPE, true, false, false,
            join(GNIRSParams.Disperser.values(), GNIRSParams.SlitWidth.values(), new Enum<?>[]{GnirsOiwfsGuideProbe.instance}),
            join(GNIRSParams.Disperser.values(), GNIRSParams.SlitWidth.values(), new Enum<?>[]{GnirsOiwfsGuideProbe.instance}),
            join(GNIRSParams.CrossDispersed.values(), GNIRSParams.Filter.values(), GNIRSParams.Camera.values())),

    GPI(Gpi.SP_TYPE, false, true, false,
            new Enum<?>[0],
            new Enum<?>[0],
            join(Gpi.Disperser.values(), Gpi.Filter.values())),

    GSAOI(Gsaoi.SP_TYPE, false, true, true,
            GsaoiOdgw.values(), GsaoiOdgw.values(), Gsaoi.Filter.values()),

    MICHELLE(InstMichelle.SP_TYPE, true, false, false),

    NICI(InstNICI.SP_TYPE, false, true, false,
            new Enum<?>[]{NiciOiwfsGuideProbe.instance},
            new Enum<?>[]{NiciOiwfsGuideProbe.instance},
            join(NICIParams.FocalPlaneMask.values(), NICIParams.DichroicWheel.values(), NICIParams.Channel1FW.values(), NICIParams.Channel2FW.values())),

    NIFS(InstNIFS.SP_TYPE, true, false, false,
            new Enum<?>[]{NifsOiwfsGuideProbe.instance},
            new Enum<?>[]{NifsOiwfsGuideProbe.instance},
            join(NIFSParams.Disperser.values(), NIFSParams.Filter.values(), NIFSParams.Mask.values())),

    NIRI(InstNIRI.SP_TYPE, true, false, false,
            join(new Enum<?>[]{NiriOiwfsGuideProbe.instance}, Niri.Filter.values()),
            join(new Enum<?>[]{NiriOiwfsGuideProbe.instance}),
            join(Niri.Mask.values(), Niri.Disperser.values(), Niri.Camera.values())),

    // Not currently in use
    PHOENIX(InstPhoenix.SP_TYPE, false, true, true),

    PWFS(SPComponentType.QPT_PWFS, // new SPInstComponentType(SPInstComponentType.INST_BROAD_TYPE, "PWFS", "PWFS"),
            true, true, true, PwfsGuideProbe.values(), PwfsGuideProbe.values()),

    // Not currently in use
    TEXES(InstTexes.SP_TYPE, false, false, false,
            new Enum<?>[0], new Enum<?>[0], TexesParams.Disperser.values()),

    TRECS(InstTReCS.SP_TYPE, false, true, false,
        join(TReCSParams.Disperser.values(), TReCSParams.Mask.values()),
        join(TReCSParams.Disperser.values(), TReCSParams.Mask.values())),


    VISITOR(SPComponentType.INSTRUMENT_VISITOR, true, true, false)
    ;

    // Some instruments have different categories of options. This map allows
    // us to assign a label to each type. If an option's class isn't in this map
    // the category will be null (which is ok).
    private static final Map<Class<? extends Enum<?>>, String> CATEGORY_MAP = new HashMap<>();
    static {

        CATEGORY_MAP.put(Flamingos2.FPUnit.class, "Focal Plane Units");
        CATEGORY_MAP.put(Flamingos2.Filter.class, "Filters");

        // GMOS-N
        CATEGORY_MAP.put(FPUnitNorth.class, "Focal Plane Units");
        CATEGORY_MAP.put(DisperserNorth.class, "Dispersers");
        CATEGORY_MAP.put(FilterNorth.class, "Filters");

        // GMOS-S
        CATEGORY_MAP.put(FPUnitSouth.class, "Focal Plane Units");
        CATEGORY_MAP.put(DisperserSouth.class, "Dispersers");
        CATEGORY_MAP.put(FilterSouth.class, "Filters");

        // GMOS-N/S
        CATEGORY_MAP.put(GmosCommonType.DetectorManufacturer.class, "CCD Manufacturer");

        // GNIRS
        CATEGORY_MAP.put(GNIRSParams.Disperser.class, "Dispersers");
        CATEGORY_MAP.put(GNIRSParams.SlitWidth.class, "Slits");

        // TRECS
        CATEGORY_MAP.put(TReCSParams.Disperser.class, "Dispersers");
        CATEGORY_MAP.put(TReCSParams.Mask.class, "Slits");
    }

    // Some enum types represent custom masks
    private static final Set<Enum<?>> CUSTOM_MASKS = new HashSet<>();
    static {

        CUSTOM_MASKS.add(FPUnitNorth.CUSTOM_MASK);
        CUSTOM_MASKS.add(FPUnitSouth.CUSTOM_MASK);

    }

    private final SPComponentType spType;
    private final boolean north, south;
    private final boolean normallyAvailable;
    private final Enum<?>[] options, normallyAvailableOptions, hiddenOptions;
    private final GuideProbe guideProbe;

    Inst(SPComponentType spType, boolean north, boolean south, boolean normallyAvailable) {
        this(spType, north, south, normallyAvailable, new Enum<?>[0], new Enum<?>[0]);
    }

    Inst(SPComponentType spType, boolean north, boolean south, boolean normallyAvailable,
         Enum<?>[] options, Enum<?>[] normallyAvailableOptions) {
        this(spType, north, south, normallyAvailable, options, normallyAvailableOptions, new Enum<?>[0]);
    }

    Inst(SPComponentType spType, boolean north, boolean south, boolean normallyAvailable,
         Enum<?>[] options, Enum<?>[] normallyAvailableOptions, Enum<?>[] hiddenOptions) {

        assert spType != null;

        this.spType = spType;
        this.north = north;
        this.south = south;
        this.normallyAvailable = normallyAvailable;
        this.options = options;
        this.normallyAvailableOptions = normallyAvailableOptions;
        this.hiddenOptions = hiddenOptions;
        this.guideProbe = null;
    }

    public boolean isNormallyAvailable() {
        return normallyAvailable;
    }

    public boolean isNormallyAvailable(Enum<?> option) {
        for (Enum<?> o: normallyAvailableOptions)
            if (o.equals(option)) return true;
        return false;
    }

    public SPComponentType getSpType() {
        return spType;
    }

    public GuideProbe getGuideProbe() {
        return guideProbe;
    }

    public boolean existsAtSite(Site site) {
        return
            (Site.GS.equals(site) && south) ||
            (Site.GN.equals(site) && north);
    }

    /**
     * Returns this for instruments, or guideProbe for WFS
     */
    public Enum<?> getValue() {
        if (getGuideProbe() instanceof Enum<?>) return (Enum<?>) getGuideProbe();
        return this;
    }

    public Enum<?>[] getOptions() {
        return options;
    }

    /**
     * Hidden options are options that should be treated as always selected, but are not shown for filtering.
     */
    public Enum<?>[] getHiddenOptions() {
        return hiddenOptions;
    }

    public static String getCategory(Enum<?> e) {
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            String cat = CATEGORY_MAP.get(c);
            if (cat != null)
                return cat;
        }
        return null;
    }

    public static Inst forSpType(SPComponentType type) {
        for (Inst f: values())
            if (type.equals(f.getSpType())) return f;
        throw new NoSuchElementException("Unknown " + Inst.class.getSimpleName() + ": " + type);
    }

    public static boolean isCustomMask(Enum<?> e) {
        return CUSTOM_MASKS.contains(e);
    }

    public static Enum<?>[] join(Enum<?>[]... arrays) {
        int length = 0;
        for (Enum<?>[] array: arrays) length += array.length;
        Enum<?>[] ret = new Enum<?>[length];
        int i = 0;
        for (Enum<?>[] array: arrays)
            for (Enum<?> e: array)
                ret[i++] = e;
        return ret;
    }
    public static Enum<?>[] arrayOf(Enum<?>... arrays) {
        return arrays;
    }
    @Override
    public String toString() {
        if (getSpType() != null) {
            if (getSpType() == InstAltair.SP_TYPE) {
                return "Altair"; // REL-293
            }
            return getSpType().readableStr;
        }
        if (getGuideProbe() != null) {
            return getGuideProbe().toString();
        }
        return super.toString(); // should not happen
    }
}
