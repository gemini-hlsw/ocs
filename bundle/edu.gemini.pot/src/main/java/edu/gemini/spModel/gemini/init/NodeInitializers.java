package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.conflict.ConflictFolder;
import edu.gemini.spModel.conflict.ConflictFolderNI;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.gemini.acqcam.SeqConfigAcqCam;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.calunit.SeqConfigCalUnit;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.SeqConfigFlamingos2;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.bhros.SeqConfigBHROS;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.ghost.SeqConfigGhost;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gmos.SeqConfigGmosNorth;
import edu.gemini.spModel.gemini.gmos.SeqConfigGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRSNI;
import edu.gemini.spModel.gemini.gnirs.SeqConfigGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gpi.SeqConfigGpi;
import edu.gemini.spModel.gemini.gpi.SeqRepeatGpiOffset;
import edu.gemini.spModel.gemini.gpol.SeqConfigGPOL;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiSeqConfig;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.SeqConfigMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.SeqConfigNICI;
import edu.gemini.spModel.gemini.nici.SeqRepeatNiciOffset;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.InstEngNifs;
import edu.gemini.spModel.gemini.nifs.SeqConfigNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.SeqConfigNIRI;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.phoenix.SeqConfigPhoenix;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.gemini.seqcomp.GhostSeqRepeatFlatObs;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatSmartGcalObs;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.InstEngTReCS;
import edu.gemini.spModel.gemini.trecs.SeqConfigTReCS;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.ProgramNote;
import edu.gemini.spModel.obscomp.SchedNote;
import edu.gemini.spModel.obscomp.SPDataOnly;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.seqcomp.*;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.template.TemplateFolder;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.shared.util.immutable.Option;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The collection of all default node initializers used by the science program
 * factory.
 */
public enum NodeInitializers {
    instance;

    public final ISPNodeInitializer<ISPConflictFolder, ConflictFolder> conflict =
            ConflictFolderNI.instance;

    public final ISPNodeInitializer<ISPGroup, SPGroup> group =
            SPGroup.NI;

    public final ISPNodeInitializer<ISPObservation, SPObservation> obsNoInstrument =
            ObservationNI.NO_INSTRUMENT;

    private final Map<Instrument, ISPNodeInitializer<ISPObservation, SPObservation>> obsInitMap;

    {
        // Default values, one for each instrument type.
        final Map<Instrument, ISPNodeInitializer<ISPObservation, SPObservation>> m =
                Arrays.stream(Instrument.values()).collect(Collectors.toMap(
                    Function.identity(),
                        ObservationNI::forInstrument));

        // Replace default values with more specific initializers as necessary.
        m.put(Instrument.Gsaoi, Gsaoi.OBSERVATION_NI);
        m.put(Instrument.Ghost, Ghost.OBSERVATION_NI());

        obsInitMap = Collections.unmodifiableMap(m);
    }

    public final ISPNodeInitializer<ISPObservation, SPObservation> obs(Option<Instrument> inst) {
        return inst.map(obsInitMap::get).getOrElse(obsNoInstrument);
    }

    public final ISPNodeInitializer<ISPObsExecLog, ObsExecLog> obsExecLog =
            ObsExecLog.NI;

    public final ISPNodeInitializer<ISPObsQaLog, ObsQaLog> obsQaLog =
            ObsQaLog.NI;

    public final ISPNodeInitializer<ISPProgram, SPProgram> program =
            ProgramNI.instance;

    public final ISPNodeInitializer<ISPNightlyRecord, NightlyRecord> record =
            NightlyRecord.NI;

    public final ISPNodeInitializer<ISPTemplateFolder, TemplateFolder> templateFolder =
            TemplateFolder.NI;

    public final ISPNodeInitializer<ISPTemplateGroup, TemplateGroup> templateGroup =
            TemplateGroup.NI;

    public final ISPNodeInitializer<ISPTemplateParameters, TemplateParameters> templateParameters =
            TemplateParameters.NI;

    public final Map<SPComponentType, ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject>> obsComp =
            Collections.unmodifiableMap(
                Stream.of(
                    Flamingos2.NI,
                    Gems.NI,
                    Ghost.NI(),
                    Gpi.NI,
                    Gsaoi.NI,
                    InstAcqCam.NI,
                    InstAltair.NI,
                    InstBHROS.NI,
                    InstEngNifs.NI,
                    InstEngTReCS.NI,
                    InstGmosNorth.NI,
                    InstGmosSouth.NI,
                    InstGNIRSNI.instance,
                    Igrins2.NI(),
                    InstMichelle.NI,
                    InstNICI.NI,
                    InstNIFS.NI,
                    InstNIRI.NI,
                    InstPhoenix.NI,
                    InstTexes.NI,
                    InstTReCS.NI,
                    ProgramNote.NI,
                    SchedNote.NI,
                    SPDataOnly.NI,
                    SPNote.NI,
                    SPSiteQuality.NI,
                    TargetObsComp.NI,
                    VisitorInstrument.NI
                ).collect(
                    Collectors.toMap(ISPNodeInitializer::getType, Function.identity())
                )
            );

    public final Map<SPComponentType, ISPNodeInitializer<ISPSeqComponent, ? extends ISPSeqObject>> seqComp =
            Collections.unmodifiableMap(
                Stream.of(
                    GsaoiSeqConfig.NI,
                    SeqBase.NI,
                    SeqConfigAcqCam.NI,
                    SeqConfigBHROS.NI,
                    SeqConfigCalUnit.NI,
                    SeqConfigFlamingos2.NI,
                    SeqConfigGhost.NI,
                    SeqConfigGmosNorth.NI,
                    SeqConfigGmosSouth.NI,
                    SeqConfigGNIRS.NI,
                    SeqConfigGpi.NI,
                    SeqConfigGPOL.NI,
                    SeqConfigMichelle.NI,
                    SeqConfigNICI.NI,
                    SeqConfigNIRI.NI,
                    SeqConfigNIFS.NI,
                    SeqConfigPhoenix.NI,
                    SeqConfigTReCS.NI,
                    SeqRepeat.NI,
                    SeqRepeatBiasObs.NI,
                    GhostSeqRepeatDarkObs.NI,
                    SeqRepeatDarkObs.NI,
                    SeqRepeatFlatObs.NI,
                    GhostSeqRepeatFlatObs.NI,
                    SeqRepeatObserve.NI,
                    SeqRepeatOffset.NI,
                    SeqRepeatGpiOffset.NI,
                    SeqRepeatNiciOffset.NI,
                    SeqRepeatSmartGcalObs.Arc.NI,
                    SeqRepeatSmartGcalObs.BaselineDay.NI,
                    SeqRepeatSmartGcalObs.BaselineNight.NI,
                    SeqRepeatSmartGcalObs.Flat.NI
                ).collect(
                    Collectors.toMap(ISPNodeInitializer::getType, Function.identity())
                )
            );
}
