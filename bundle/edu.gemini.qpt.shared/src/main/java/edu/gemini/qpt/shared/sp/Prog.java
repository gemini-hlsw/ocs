package edu.gemini.qpt.shared.sp;

import edu.gemini.spModel.core.*;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

import java.io.Serializable;
import java.util.*;

/**
 * Mini-model representation of a science program.
 * <p>
 */
public final class Prog implements Serializable, Comparable<Prog> {

    private static final long serialVersionUID = 1L;

    private final SPProgramID programId;
    private final ProgramId structuredProgramId;
    private final String title;
    private final Affiliate partner;
    private final int band;
    private final boolean completed;
    private final boolean rollover;
    private final boolean active;
    private final long plannedTime;
    private final long usedTime;
    private final long remainingTime;
    private final Long band3MinimumTime;
    private final Long band3RemainingTime;
    private final String piLastName;
    private final String ngoEmail;
    private final String contactEmail;

    private SortedSet<Obs> fullObsSet;
    private List<Obs> obsList;
    private List<Group> groupList;
    private List<Note> noteList;

    /**
     * In order to get the back pointers working while keeping the mini-model
     * immutable, we have to construct programs in two steps. First construct with
     * basic program info, then call setChildren() exactly once.
     *
     * @param program
     * @param programId
     * @param active
     * @param band
     * @param rollover
     * @param plannedTime
     * @param usedTime
     * @param remainingTime
     * @param band3MinimumTime
     * @param band3RemainingTime
     * @param piLastName
     * @param ngoEmail
     * @param contactEmail
     */
    public Prog(SPProgram program, SPProgramID programId, boolean active, int band, boolean rollover, long plannedTime, long usedTime, long remainingTime, Long band3MinimumTime, Long band3RemainingTime, String piLastName, String ngoEmail, String contactEmail) {
        this.title = program.getTitle();
        this.partner = program.getPIAffiliate();
        this.programId = programId;
        this.structuredProgramId = ProgramId$.MODULE$.parse(programId.stringValue());
        this.completed = program.isCompleted();
        this.active = active;
        this.band = band;
        this.rollover = rollover;
        this.plannedTime = plannedTime;
        this.usedTime = usedTime;
        this.remainingTime = remainingTime;
        this.band3MinimumTime = band3MinimumTime;
        this.band3RemainingTime = band3RemainingTime;
        this.piLastName = piLastName;
        this.ngoEmail = ngoEmail;
        this.contactEmail = contactEmail;
    }

    /**
     * Special constructor used by the Find functionality to construct arbitrary new Prog shells. Sketchy.
     *
     * @param programId
     */
    public Prog(SPProgramID programId) {
        this.title = null;
        this.partner = null;
        this.programId = programId;
        this.structuredProgramId = ProgramId$.MODULE$.parse(programId.stringValue());
        this.completed = false;
        this.active = false;
        this.band = 0;
        this.rollover = false;
        this.plannedTime = 0;
        this.usedTime = 0;
        this.remainingTime = 0;
        this.band3MinimumTime = null;
        this.band3RemainingTime = null;
        this.piLastName = "";
        this.ngoEmail = "";
        this.contactEmail = "";
    }

    void setChildren(List<Obs> obsList, List<Group> groupList, List<Note> noteList) {
        if (this.obsList != null)
            throw new IllegalStateException("setChildren has already been called.");

        this.obsList = Collections.unmodifiableList(new ArrayList<Obs>(obsList));
        this.groupList = Collections.unmodifiableList(new ArrayList<Group>(groupList));
        this.noteList = Collections.unmodifiableList(new ArrayList<Note>(noteList));

        // Construct the full obs set.
        SortedSet<Obs> accum = new TreeSet<Obs>();
        accum.addAll(obsList);
        for (Group g : groupList) {
            accum.addAll(g.getObservations());
        }
        fullObsSet = Collections.unmodifiableSortedSet(accum);
    }

    public int getBand() {
        return band;
    }

    // TODO: getBandEnum and getBand can/should probably be unified, some refactorings in QPT needed
    public Band getBandEnum() {
        if (programId.isClassical()) return Band.Undefined;
        else switch (getBand()) {
            case 1:
                return Band.Band1;
            case 2:
                return Band.Band2;
            case 3:
                return Band.Band3;
            case 4:
                return Band.Band4;
        }
        // none of the above? don't know what that is..
        return Band.Undefined;
    }

    public boolean getRollover() {
        return rollover;
    }

    public int compareTo(Prog o) {
        return programId.compareTo(o.programId);
    }

    public SPProgramID getProgramId() {
        return programId;
    }

    public ProgramId getStructuredProgramId() {
        return structuredProgramId;
    }

    public scala.Option<ProgramType> getType() {
        return structuredProgramId.ptype();
    }

    public Option<ProgramType> getTypeAsJava() {
        return ImOption.fromScalaOpt(getType());
    }

    public boolean isType(ProgramTypeEnum e) {
        return getTypeAsJava().exists(t -> t.typeEnum() == e);
    }

    public scala.Option<Semester> getSemester() {
        return structuredProgramId.semester();
    }

    public Option<Semester> getSemesterAsJava() {
        return ImOption.fromScalaOpt(getSemester());
    }

    public String getTitle() {
        return title;
    }

    public Affiliate getPartner() {
        return partner;
    }

    public long getPlannedTime() {
        return plannedTime;
    }

    public long getUsedTime() {
        return usedTime;
    }

    public long getRemainingProgramTime() {
        return remainingTime;
    }

    public Long getBand3MinimumTime() {
        return band3MinimumTime;
    }

    public Long getBand3RemainingTime() {
        return band3RemainingTime;
    }

    public String getPiLastName() {
        return piLastName;
    }

    public String getNgoEmail() {
        return ngoEmail;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isActive() {
        return active;
    }

    public SortedSet<Obs> getFullObsSet() {
        assert fullObsSet != null;
        return fullObsSet;
    }

    public List<Obs> getObsList() {
        assert obsList != null;
        return obsList;
    }

    public List<Group> getGroupList() {
        assert groupList != null;
        return groupList;
    }

    public List<Note> getNoteList() {
        assert noteList != null;
        return noteList;
    }

    @Override
    public String toString() {
        return structuredProgramId.shortName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Prog && programId.equals(((Prog) obj).programId);
    }

    @Override
    public int hashCode() {
        return programId.hashCode();
    }

    public boolean isEngOrCal() {
        return isType(ProgramTypeEnum.ENG) || isType(ProgramTypeEnum.CAL);
    }

}
