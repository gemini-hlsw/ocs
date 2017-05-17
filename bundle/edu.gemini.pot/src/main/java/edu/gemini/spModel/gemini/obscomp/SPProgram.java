// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPProgram.java 45577 2012-05-28 23:58:49Z swalker $
//
package edu.gemini.spModel.gemini.obscomp;

import edu.gemini.pot.sp.ISPStaffOnlyFieldProtected;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.shared.util.CalendarUtil;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.dataflow.GsaAspect;
import edu.gemini.spModel.gemini.phase1.GsaPhase1Data;
import edu.gemini.spModel.pio.*;
import edu.gemini.spModel.timeacct.TimeAcctAllocation;
import edu.gemini.spModel.timeacct.TimeAcctPio;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.type.DescribableSpType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.util.ObjectUtil;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.*;

/**
 * The SPProgram item.  Data associated with the SPProgram node
 * in a Science Program.
 * @author	Kim Gillies
 */
public class SPProgram extends AbstractDataObject implements ISPStaffOnlyFieldProtected {
    private static final Logger LOG = Logger.getLogger(SPProgram.class.getName());

    public static final SPComponentType SP_TYPE = SPComponentType.PROGRAM_BASIC;

    // for serialization
    private static final long serialVersionUID = 4L;

    public static final String VERSION = "2017B-1";

    /** This property records the program queue/classical state. */
    public static final String PROGRAM_MODE_PROP = "programMode";

    /** This property records the program status. */
    public static final String PROGRAM_STATUS_PROP = "programStatus";

    // The PI information
    /** This property is the Principle Investigator info */
    public static final String PI_INFO_PROP = "piInfo";

    /** This property is the name of the contact person. */
    public static final String CONTACT_PERSON_PROP = "contactPerson";

    /** The default value for the contact person **/
    public static final String DEFAULT_CONTACT_PERSON = EMPTY_STRING;

    /** This property is the name of the NGO contact person email */
    public static final String NGO_CONTACT_EMAIL_PROP_OLD = "gnoEmail"; // bugfix: keep for backward compat

    public static final String NGO_CONTACT_EMAIL_PROP = "ngoEmail";

    /** The default value for the primary contact email **/
    public static final String DEFAULT_PRIMARY_CONTACT_EMAIL = EMPTY_STRING;

    /** The default final active date */
    public static final Date DEFAULT_FINAL_ACTIVE_DATE = CalendarUtil.newDate(Calendar.JANUARY, 1, 2005);

    /** This property is the queue band it only appears for queue mode */
    public static final String QUEUE_BAND_PROP = "queueBand";

    /** The default value for the queue band */
    public static final String DEFAULT_QUEUE_BAND = EMPTY_STRING;

    /** This property is the sched dates it only appears for classical mode */
    public static final String SCHED_DATES_PROP = "schedDates";

    /** The default value for the sched dates List */
    public static final List<String> DEFAULT_SCHED_DATES = null;

    /** This property is the time awarded by the TACs */
    public static final String AWARDED_TIME_PROP = "awardedTime";

    /** This attribute indicates the minimum acceptable time for band 3 progs */
    public static final String MINIMUM_TIME_PROP = "minimumTime";

    /** This property is fired when the final active date is changed */
    public static final String FINAL_ACTIVE_DATE_PROP = "finalActiveDate";

    /** This property is used internally to track observation ids */
    public static final String OBSID_PROP = "nextObsId";

    /** This attribute indicates if it is a active program */
    public static final String ACTIVE_PROP = "active";

    // obfuscate the active flag in the XML (SCT-163)
    public static final String ACTIVE_PROP_OBFUSCATED = "fetched";

    /** This attribute indicates if the program is completed */
    public static final String COMPLETED_PROP = "completed";

    /** This attribute indicates if the PI should be notified when the obs is done */
    public static final String NOTIFY_PI_PROP = "notifyPi";

    /** This attribute indicates the rollover status of this program */
    public static final String ROLLOVER_FLAG_PROP = "rolloverFlag";

    /** This attribute indicates whether the program is related to a thesis. */
    public static final String THESIS_FLAG_PROP = "isThesis";
    public static final String LIBRARY_FLAG_PROP = "isLibrary";

    public static final String GSA_PROP = "gsa";

    public static final String TOO_TYPE_PROP = "tooType";

    /**
     * Program status values.
     */
    public enum ProgramStatus implements DisplayableSpType, DescribableSpType {
        NEW("New", "Created by New"),
        PHASE1("Phase 1", "Initialized from Phase 1"),
        PHASE2("Phase 2", "In Phase 2 preparation"),
        STARTED("Started", "One or more observations exected"),
        COMPLETED("Completed", "All observations exected"),
        SENT("Sent", "All data sent to PI"),
        ;

        /** The default ProgramStatus value **/
        public static ProgramStatus DEFAULT = PHASE2;

        private String _name;
        private String _desc;

        ProgramStatus(String name, String description) {
            _name = name;
            _desc = description;
        }

        public String displayValue() {
            return _name;
        }

        public String description() {
            return _desc;
        }

        /** Return a ProgramStatus by code **/
        public static ProgramStatus getProgramStatusByIndex(int i) {
            return SpTypeUtil.valueOf(ProgramStatus.class, i, DEFAULT);
        }

        /** Return a ProgramStatus by name **/
        public static ProgramStatus getProgramStatus(String name) {
            return getProgramStatus(name, DEFAULT);
        }

        /** Return a ProgramStatus by name with a value to return upon error **/
        public static ProgramStatus getProgramStatus(String name, ProgramStatus nvalue) {
            return SpTypeUtil.oldValueOf(ProgramStatus.class, name, nvalue);
        }
    }

    /**
     * Program execution models
     */
    public enum ProgramMode implements DisplayableSpType {
        QUEUE("Queue"),
        CLASSICAL("Classical"),
        ;

        /** The default ProgramMode value **/
        public static ProgramMode DEFAULT = QUEUE;

        private String _displayValue;

        ProgramMode(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        /**
         * Is the mode queue?
         * @return true if it is a queue mode observation.
         */
        public boolean isQueue() {
            return this == QUEUE;
        }

        /**
         * Is the mode classical?
         * @return true if it is a classical mode observation.
         */
        public boolean isClassical() {
            return this == CLASSICAL;
        }

        /** Return a ProgramMode by name **/
        public static ProgramMode getProgramMode(String name) {
            return getProgramMode(name, DEFAULT);
        }

        /** Return a ProgramMode by name with a value to return upon error **/
        public static ProgramMode getProgramMode(String name, ProgramMode nvalue) {
            return SpTypeUtil.oldValueOf(ProgramMode.class, name, nvalue);
        }
    }

    // The  PI Info
    public static final class PIInfo implements Cloneable, Serializable {

        // for serialization
        private static final long serialVersionUID = 1L;

        private static final int DEFAULT_INDEX = 0;

        // paramset tags
        private static final String _FIRST_NAME = "firstName";
        private static final String _LAST_NAME = "lastName";
        private static final String _EMAIL = "email";
        private static final String _PHONE = "phone";
        private static final String _AFFILIATE = "affiliate";

        // default values
        public static final String DEFAULT_LASTNAME = EMPTY_STRING;
        public static final String DEFAULT_FIRSTNAME = EMPTY_STRING;
        public static final String DEFAULT_EMAIL = EMPTY_STRING;
        public static final String DEFAULT_PHONE = EMPTY_STRING;

        // The pi last, first name, email and phone
        private String _lastName = DEFAULT_LASTNAME;
        private String _firstName = DEFAULT_FIRSTNAME;
        private String _email = DEFAULT_EMAIL;
        private String _phone = DEFAULT_PHONE;
        private Affiliate _affiliate = null;

        /**
         * The default constructor.  All fields default to the
         * empty String.
         */
        public PIInfo() {
        }

        public PIInfo(ParamSet pset) {
            if (pset == null) return;
            _firstName = Pio.getValue(pset, _FIRST_NAME);
            _lastName = Pio.getValue(pset, _LAST_NAME);
            _email = Pio.getValue(pset, _EMAIL);
            _phone = Pio.getValue(pset, _PHONE);
            _affiliate = Affiliate.fromString(Pio.getValue(pset, _AFFILIATE));
        }

        /** The constructor for PIInfo **/
        public PIInfo(String firstName, String lastName, String email, String phone, Affiliate affiliate) {
            _firstName = firstName;
            _lastName = lastName;
            _email = email;
            _phone = phone;
            _affiliate = affiliate;
        }

        /**
         * Print out PI info for diags.
         */
        public String toString() {
            return getFirstName() + " " + getLastName() + " " + getEmail() + " " + getPhone() + " " + getAffiliate();
        }

        /** Clone **/
        public Object clone() {
            PIInfo result;
            try {
                result = (PIInfo) super.clone();
            } catch (CloneNotSupportedException ex) {
                // Won't happen, since Object implements cloneable ...
                throw new InternalError();
            }
            // all are immutable
            return result;
        }

        /** Return the first name **/
        public String getFirstName() {
            return _firstName;
        }

        /** Return the last name **/
        public String getLastName() {
            return _lastName;
        }

        /** Return the PI email **/
        public String getEmail() {
            return _email;
        }

        /** Return the PI phone **/
        public String getPhone() {
            return _phone;
        }

        /** Return the PI affiliation **/
        public Affiliate getAffiliate() {
            return _affiliate;
        }

        public PIInfo withAffiliate(Affiliate affiliate) {
            return new PIInfo(_firstName, _lastName, _email, _phone, affiliate);
        }

        /**
         * Return a parameter set describing the current state of this object.
         */
        public ParamSet getParamSet(PioFactory factory, String name) {
            ParamSet paramSet = factory.createParamSet(name);

            Pio.addParam(factory, paramSet, _FIRST_NAME, _firstName);
            Pio.addParam(factory, paramSet, _LAST_NAME, _lastName);
            Pio.addParam(factory, paramSet, _EMAIL, _email);
            Pio.addParam(factory, paramSet, _PHONE, _phone);
            if (_affiliate != null) {
                Pio.addParam(factory, paramSet, _AFFILIATE, _affiliate.name());
            }

            return paramSet;
        }
    }

    /**
     * Values (yes/no), to indicate if it is a active program
     */
    public enum Active implements DisplayableSpType {

        NO("No") {
            public String getObfuscatedValue() {
                return "yes";
            }
        },
        YES("Yes") {
            public String getObfuscatedValue() {
                return "true";
            }
        };

        public static Active DEFAULT = YES;

        private String _displayValue;

        Active(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public static Active getActiveByIndex(int index) {
            return SpTypeUtil.valueOf(Active.class, index, DEFAULT);
        }

        public static Active getActive(String name) {
            return getActive(name, DEFAULT);
        }

        public static Active getActive(String name, Active nvalue) {
            return SpTypeUtil.oldValueOf(Active.class, name, nvalue);
        }

        public abstract String getObfuscatedValue();

        public static Active parseObfuscatedValue(String value) {
            return Boolean.valueOf(value) ? YES : NO;
        }
    }

    // The rollover flag for this particular program
    private boolean _rollover = false;

    // Whether the program is associated with a thesis
    private boolean _isThesis = false;

    private boolean _isLibrary = false;

    // The internal state of the program mode
    private ProgramMode _programMode = ProgramMode.DEFAULT;

    // The internal value for ProgramStatus
    private ProgramStatus _programStatus = ProgramStatus.DEFAULT;

    /** The default value for the PI Info. **/
    public final PIInfo DEFAULT_PI_INFO = null;

    // The internal value for the property.
    private PIInfo _piInfo = DEFAULT_PI_INFO;

    // The internal value.
    private String _contactPerson = DEFAULT_CONTACT_PERSON;

    /** The internal value **/
    private String _primaryContactEmail = DEFAULT_PRIMARY_CONTACT_EMAIL;

    // The internal value
    private String _queueBand = DEFAULT_QUEUE_BAND;

    // The internal value
    private List<String> _schedDates = DEFAULT_SCHED_DATES;

    // SCT-332: time allocation stored amongst time accounting categories.
    private TimeAcctAllocation _timeAllocation = TimeAcctAllocation.EMPTY;

    // SCT-374: minimum time allocation for band 3 programs
    private TimeValue _minTimeValue;

    // The next observation to be used for this observation
    private int _obsCounter;

    // Indicates if this is an active program
    private Active _active = Active.DEFAULT;

    // Indicates if this program is completed
    private boolean _completed = false;

    // Indicates if the PI should be notified when the observation is done
    private YesNoType _notifyPi = YesNoType.YES; // REL-1150

    // indicates the final date that the program is active in the database.
    // This will be extended by the ITAC rearranging software when a program from the
    // previous semester is to be "rolled over".
    private Date _finalActiveDate = DEFAULT_FINAL_ACTIVE_DATE;

    private GsaAspect _gsa;
    private GsaPhase1Data _gsaPhase1Data;

    // SW: prior to 2012B, this was extracted from the old Phase 1 document
    private TooType _too = TooType.none;

    /**
     * Default constructor.
     */
    public SPProgram() {
        super(SP_TYPE);
        _obsCounter = 1;
        // As of 2006B, we'll be using the hash of the program id for the
        // password.  Programs without ids simply won't ever match any key.
//        _programPassword = generateNewPassword();
        setVersion(VERSION);
    }

    /**
     * Override clone to make sure the list members are copied correctly.
     */
    public Object clone() {
        SPProgram prog = (SPProgram)super.clone();

        if (_piInfo != null) {
            prog._piInfo = (PIInfo)_piInfo.clone();
        }

        return prog;
    }

    @Override public boolean staffOnlyFieldsEqual(ISPDataObject to) {
        final SPProgram that = (SPProgram) to;

        final Affiliate thisPiAff = (_piInfo == null) ? null : _piInfo._affiliate;
        final Affiliate thatPiAff = (that._piInfo == null) ? null : that._piInfo._affiliate;

        return thisPiAff == thatPiAff &&
               ObjectUtil.equals(_contactPerson, that._contactPerson) &&
               _rollover == that._rollover &&
               _isThesis == that._isThesis &&
               _isLibrary == that._isLibrary &&
               _programMode == that._programMode &&
               _programStatus == that._programStatus &&
               ObjectUtil.equals(_queueBand, that._queueBand) &&
               ObjectUtil.equals(_schedDates, that._schedDates) &&
               ObjectUtil.equals(_timeAllocation, that._timeAllocation) &&
               ObjectUtil.equals(_minTimeValue, that._minTimeValue) &&
               _active == that._active &&
               _completed == that._completed &&
               ObjectUtil.equals(_finalActiveDate, that._finalActiveDate) &&
               ObjectUtil.equals(_gsa, that._gsa) &&
               ObjectUtil.equals(_gsaPhase1Data, that._gsaPhase1Data) &&
               _too == that._too;

    }

    @Override public boolean staffOnlyFieldsDefaulted() {
        return staffOnlyFieldsEqual(new SPProgram());
    }

    @Override public void setStaffOnlyFieldsFrom(ISPDataObject to) {
        final SPProgram that = (SPProgram) to;

        // A PI can't edit his email or his affiliate
        if (_piInfo == null) _piInfo = that._piInfo;
        if (that._piInfo != null) {
            _piInfo._affiliate = that._piInfo._affiliate;
        }
        _contactPerson   = that._contactPerson;

        _rollover        = that._rollover;
        _isThesis        = that._isThesis;
        _isLibrary       = that._isLibrary;
        _programMode     = that._programMode;
        _programStatus   = that._programStatus;
        _queueBand       = that._queueBand;
        _schedDates      = that._schedDates;
        _timeAllocation  = that._timeAllocation;
        _minTimeValue    = that._minTimeValue;
        _active          = that._active;
        _completed       = that._completed;
        _finalActiveDate = that._finalActiveDate;
        _gsa             = that._gsa;
        _gsaPhase1Data   = that._gsaPhase1Data;
        _too             = that._too;
    }

    @Override public void resetStaffOnlyFieldsToDefaults() {
        setStaffOnlyFieldsFrom(new SPProgram());
    }


    public TooType getTooType() {
        return _too == null ? TooType.none : _too;
    }

    public void setTooType(TooType newValue) {
        TooType oldValue = _too;
        if (oldValue != newValue) {
            _too = newValue;
            firePropertyChange(TOO_TYPE_PROP, oldValue, newValue);
        }
    }

    public boolean isToo() {
        return _too != TooType.none;
    }

    /**
     * Get the program mode.
     */
    public ProgramMode getProgramMode() {
        return _programMode;
    }

    /**
     * Get the program mode as a string.
     */
    public String getProgramModeAsString() {
        return getProgramMode().name();
    }

    /**
     * Get the rollover status
     */
    public boolean getRolloverStatus() {
        return _rollover;
    }

    /**
     * Set the rollover status
     */
    public void setRolloverStatus(boolean value) {
        _rollover = value;
    }

    /**
     * Get the thesis status.
     */
    public boolean isThesis() {
        return _isThesis;
    }

    /**
     * Set the thesis status.
     */
    public void setThesis(boolean isThesis) {
        _isThesis = isThesis;
    }

    public boolean isLibrary() {
        return _isLibrary;
    }

    public void setLibrary(boolean isLibrary) {
        _isLibrary = isLibrary;
    }

    /**
     * Set the program execution mode.
     */
    public void setProgramMode(ProgramMode newValue) {
        ProgramMode oldValue = _programMode;
        if (oldValue != newValue) {
            _programMode = newValue;
            firePropertyChange(PROGRAM_MODE_PROP, oldValue, newValue);
        }
    }


    /**
     * Get the program status state.
     *
     * @return a <code>{@link ProgramStatus}</code> object.
     */
    public ProgramStatus getProgramStatus() {
        return _programStatus;
    }

    /**
     * Get the program status as a string.
     */
    public String getProgramStatusAsString() {
        return getProgramStatus().name();
    }

    /**
     * Set the program execution status.
     */
    public void setProgramStatus(ProgramStatus newValue) {
        ProgramStatus oldValue = _programStatus;
        if (oldValue != newValue) {
            _programStatus = newValue;
            firePropertyChange(PROGRAM_STATUS_PROP, oldValue, newValue);
        }
    }

    /**
     * Return the PIInfo as a readonly item.
     */
    public PIInfo getPIInfo() {
        if (_piInfo == null) _piInfo = new PIInfo();
        return (PIInfo) _piInfo.clone();
    }

    /**
     * Sets the internal PIInfo object.
     */
    public void setPIInfo(PIInfo newValue) {
        PIInfo oldValue = _piInfo;
        _piInfo = newValue;
        firePropertyChange(PI_INFO_PROP, oldValue, newValue);
    }


    /** Return the PI's first name **/
    public String getPIFirstName() {
        if (_piInfo == null)
            return null;
        return _piInfo.getFirstName();
    }

    /** Return the PI's last name **/
    public String getPILastName() {
        if (_piInfo == null)
            return null;
        return _piInfo.getLastName();
    }


    /** Return the PI's affiliation **/
    public Affiliate getPIAffiliate() {
        if (_piInfo == null)
            return null;
        return _piInfo.getAffiliate();
    }


    // -- Is this an active program? --

    public Active getActive() {
        return _active;
    }

    public int getActiveIndex() {
        return _active.ordinal();
    }

    public boolean isActive() {
        return _active == Active.YES;
    }

    public void setActive(Active newValue) {
        if (newValue == null) {
            newValue = Active.DEFAULT;
        }
        Active oldValue = getActive();
        if (oldValue != newValue) {
            _active = newValue;
            firePropertyChange(ACTIVE_PROP, oldValue, newValue);
        }
    }

    private void _setActive(String name) {
        Active oldValue = getActive();
        setActive(Active.getActive(name, oldValue));
    }

    public boolean isCompleted() {
            return _completed;
    }

    public void setCompleted(boolean completed) {
            boolean prev = _completed;
            _completed = completed;
            getPropertyChangeSupport().firePropertyChange(COMPLETED_PROP, prev, completed);
    }

    private void _setCompleted(String name) {
            boolean completed = Boolean.valueOf(name);
            setCompleted(completed);
    }

    // -- Should the PI be notified? --

    public YesNoType getNotifyPi() {
        return _notifyPi;
    }

    public int getNotifyPiIndex() {
        return _notifyPi.ordinal();
    }

    public boolean isNotifyPi() {
        return _notifyPi == YesNoType.YES;
    }

    public void setNotifyPi(YesNoType newValue) {
        if (newValue == null) {
            newValue = YesNoType.DEFAULT;
        }
        YesNoType oldValue = getNotifyPi();
        if (oldValue != newValue) {
            _notifyPi = newValue;
            firePropertyChange(NOTIFY_PI_PROP, oldValue, newValue);
        }
    }

    private void _setNotifyPi(String name) {
        YesNoType oldValue = getNotifyPi();
        setNotifyPi(YesNoType.getYesNoType(name, oldValue));
    }



    /**
     * Returns the contact scientist for this proposal.
     */
    public String getContactPerson() {
        return _contactPerson;
    }

    /**
     * Sets the value for the contact person for this proposal.
     */
    public void setContactPerson(String newValue) {
        String oldValue = _contactPerson;
        if (!oldValue.equals(newValue)) {
            _contactPerson = newValue;
            firePropertyChange(CONTACT_PERSON_PROP, oldValue, newValue);
        }
    }

    /**
     * Returns the email for the primary contact scientist for this proposal.
     */
    public String getPrimaryContactEmail() {
        return _primaryContactEmail;
    }

    /**
     * Sets the value for the primary contact email person for this proposal.
     */
    public void setPrimaryContactEmail(String newValue) {
        String oldValue = _primaryContactEmail;
        if (!oldValue.equals(newValue)) {
            _primaryContactEmail = newValue;
            firePropertyChange(NGO_CONTACT_EMAIL_PROP, oldValue, newValue);
        }
    }

    /**
     * Returns the queue band for the program if it is a queue mode
     * proposal.
     * @return String version of the band, else an empty String if it
     * is a classical observation.
     */
    public String getQueueBand() {
        return _queueBand;
    }

    /**
     * Sets the queue band for this proposal.  This is quite
     * Gemini specific.
     */
    public void setQueueBand(String newValue) {
        String oldValue = _queueBand;
        if (!oldValue.equals(newValue)) {
            _queueBand = newValue;
            firePropertyChange(QUEUE_BAND_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the {@link List} of String dates which are the
     * scheduled dates.
     */
    public void setScheduledDates(List<String> newValue) {
        // note: this item is only set from the phase1 program and on XML import,
        // so there needs to be no event fired on change.
        _schedDates = new ArrayList<String>(newValue);
    }

    /**
     * Get the scheduled dates in a read-only List of Strings.
     * If there are no dates, an empty list is returned.
     */
    public List<String> getScheduledDates() {
        if (_schedDates == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(_schedDates);
    }


    /**
     * Returns the awarded time for the program.
     * @return String version of the awarded time, else the default
     * value.
     */
    public TimeValue getAwardedProgramTime() {
        if (_timeAllocation == null) return new TimeValue(0, TimeValue.Units.hours);
        return TimeValue.millisecondsToTimeValue(_timeAllocation.getSum().getProgramAward().toMillis(), TimeValue.Units.hours);
    }

    public TimeAcctAllocation getTimeAcctAllocation() {
        return _timeAllocation;
    }

    public void setTimeAcctAllocation(TimeAcctAllocation allocation) {
        if (allocation == null) allocation = TimeAcctAllocation.EMPTY;
        _timeAllocation = allocation;
    }

    public TimeValue getMinimumTime() {
        if (_minTimeValue == null) return TimeValue.ZERO_HOURS;
        return _minTimeValue;
    }

    public void setMinimumTime(TimeValue minTime) {
        _minTimeValue = minTime;
    }

    /** Return the final active date for this program. */
    public Date getFinalActiveDate() {
        if (_finalActiveDate == null) {
            _finalActiveDate = DEFAULT_FINAL_ACTIVE_DATE;
        }
        return _finalActiveDate;
    }


    /** Set the final active date for this program. */
    public void setFinalActiveDate(Date newValue) {
        Date oldValue = _finalActiveDate;
        if (oldValue == null || !oldValue.equals(newValue)) {
            _finalActiveDate = newValue;
            firePropertyChange(FINAL_ACTIVE_DATE_PROP, oldValue, newValue);
        }
    }

    public GsaAspect getGsaAspect() {
        return _gsa;
    }

    public synchronized void setGsaAspect(GsaAspect gsa) {
        GsaAspect orig = _gsa;
        _gsa = gsa;
        firePropertyChange(GSA_PROP, orig, _gsa);
    }

    public GsaPhase1Data getGsaPhase1Data() {
        return (_gsaPhase1Data == null) ? GsaPhase1Data.EMPTY : _gsaPhase1Data;
    }
    public void setGsaPhase1Data(GsaPhase1Data p1) {
        _gsaPhase1Data = p1;
    }

    /* UX-1520
    public List<String> getIgnoredProblems() {
        return _ignoredProblems == null ? null : new ArrayList<String>(_ignoredProblems);
    }

    public void setIgnoredProblems(List<String> ignoredProblems) {
        List<String> orig = _ignoredProblems;
        _ignoredProblems = ignoredProblems;
        firePropertyChange(IGNORED_PROBLEMS_PROP, orig, _ignoredProblems);
    }
    */

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, PROGRAM_MODE_PROP, getProgramModeAsString());
        Pio.addParam(factory, paramSet, TOO_TYPE_PROP, getTooType().name());
        Pio.addParam(factory, paramSet, PROGRAM_STATUS_PROP, getProgramStatusAsString());
        Pio.addParam(factory, paramSet, OBSID_PROP, Integer.toString(_obsCounter));
//        Pio.addParam(factory, paramSet, PROGRAM_PASSWORD_PROP, getProgramPassword());

        // I've done it this way since getPIInfo creates an object.
        if (_piInfo != DEFAULT_PI_INFO) {
            paramSet.addParamSet(_piInfo.getParamSet(factory, PI_INFO_PROP));
        }

        String contactPerson = getContactPerson();
        if (!contactPerson.equals(DEFAULT_CONTACT_PERSON)) {
            Pio.addParam(factory, paramSet, CONTACT_PERSON_PROP, contactPerson);
        }

        String ngoContactEmail = getPrimaryContactEmail();
        if (!ngoContactEmail.equals(DEFAULT_PRIMARY_CONTACT_EMAIL)) {
            Pio.addParam(factory, paramSet, NGO_CONTACT_EMAIL_PROP, ngoContactEmail);
        }

        // Write the band only if it is queue mode and non default
        if (getProgramMode().isQueue()) {
            String qband = getQueueBand();
            if (!DEFAULT_QUEUE_BAND.equals(qband)) {
                Pio.addParam(factory, paramSet, QUEUE_BAND_PROP, qband);
            }
        }

        // Write the rollover flag information, only if it's set to true.
        if (getRolloverStatus()) {
            String rolloverString = "true";
            Pio.addParam(factory, paramSet, ROLLOVER_FLAG_PROP, rolloverString);
        }

        // Write the rollover flag information, only if it's set to true.
        if (isThesis()) {
            Pio.addParam(factory, paramSet, THESIS_FLAG_PROP, "true");
        }

        if (isLibrary()) {
            Pio.addBooleanParam(factory, paramSet, LIBRARY_FLAG_PROP, true);
        }

        // Write the dates only if it is classical mode and non default
        if (getProgramMode().isClassical()) {
            List<String> dates = getScheduledDates();
            if (dates.size() > 0) {
                Pio.addListParam(factory, paramSet, SCHED_DATES_PROP, dates);
            }
        }

        // Write the time accounting information.
        if (_timeAllocation != null) {
            ParamSet timeAcctPset;
            timeAcctPset = TimeAcctPio.getParamSet(factory, _timeAllocation);
            paramSet.addParamSet(timeAcctPset);

            // Write the awarded time (for GSA only -- will be ignored on
            // import)
            final double hrs = _timeAllocation.getSum().getProgramHours();
            Pio.addParam(factory, paramSet, AWARDED_TIME_PROP, Double.toString(hrs), "hours");
        }
        if ((_minTimeValue != null) && (_minTimeValue.getTimeAmount() > 0)) {
            Pio.addParam(factory, paramSet, MINIMUM_TIME_PROP,
                    String.valueOf(_minTimeValue.getTimeAmount()),
                    _minTimeValue.getTimeUnits().toString());
        }

        // SCT-163 requires us to hide the active flag
        Pio.addParam(factory, paramSet, ACTIVE_PROP_OBFUSCATED, getActive().getObfuscatedValue());

        Pio.addParam(factory, paramSet, COMPLETED_PROP, Boolean.toString(isCompleted()));
        Pio.addParam(factory, paramSet, NOTIFY_PI_PROP, getNotifyPi().name());

        // SCT 201
        if (_gsa != null) {
            paramSet.addParamSet(_gsa.getParamSet(factory, GSA_PROP));
        }

        GsaPhase1Data gp1 = getGsaPhase1Data();
        if (!gp1.equals(GsaPhase1Data.EMPTY)) {
            paramSet.addParamSet(gp1.toParamSet(factory));
        }

//        UX-1520
//        if (_ignoredProblems != null && _ignoredProblems.size() != 0) {
//            Pio.addListParam(factory, paramSet, IGNORED_PROBLEMS_PROP, _ignoredProblems);
//        }

        return paramSet;
    }


    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, PROGRAM_MODE_PROP);
        if (v != null) {
            setProgramMode(ProgramMode.getProgramMode(v));
        }
        v = Pio.getValue(paramSet, TOO_TYPE_PROP);
        if (v != null) {
            setTooType(TooType.getTooType(v));
        }
        v = Pio.getValue(paramSet, PROGRAM_STATUS_PROP);
        if (v != null) {
            setProgramStatus(ProgramStatus.getProgramStatus(v));
        }
        v = Pio.getValue(paramSet, OBSID_PROP);
        if (v != null) {
            _obsCounter = Integer.parseInt(v);
        }
//        v = Pio.getValue(paramSet, PROGRAM_PASSWORD_PROP);
//        if (v != null) {
//            setProgramPassword(v);
//        }
        ParamSet p = paramSet.getParamSet(PI_INFO_PROP);
        if (p != null) {
            _piInfo = new PIInfo(p);
        }
        v = Pio.getValue(paramSet, CONTACT_PERSON_PROP);
        if (v != null) {
            setContactPerson(v);
        }
        v = Pio.getValue(paramSet, NGO_CONTACT_EMAIL_PROP);
        if (v == null) {
            v = Pio.getValue(paramSet, NGO_CONTACT_EMAIL_PROP_OLD); // for backward compat
        }
        if (v != null) {
            setPrimaryContactEmail(v);
        }
        v = Pio.getValue(paramSet, QUEUE_BAND_PROP);
        if (v != null) {
            setQueueBand(v);
        }
        //noinspection unchecked
        List<String> l = (List<String>) paramSet.getParams(SCHED_DATES_PROP);
        if (l != null) {
            setScheduledDates(l);
        }

        // SCT-332: Time acct info now stored in SPProgram itself.
        try {
            ParamSet timeAcctPset = paramSet.getParamSet(TimeAcctPio.TIME_ACCT_PARAM_SET);
            if (timeAcctPset != null) {
                _timeAllocation = TimeAcctPio.getTimeAcctAllocation(timeAcctPset);
            }
        } catch (PioParseException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
        _minTimeValue = parseTimeValueParam(paramSet, MINIMUM_TIME_PROP);

        // SCT-163 hacks ... hide the active flag
        v = Pio.getValue(paramSet, ACTIVE_PROP_OBFUSCATED);
        if (v != null) {
            setActive(Active.parseObfuscatedValue(v));
        } else {
            v = Pio.getValue(paramSet, ACTIVE_PROP);
            if (v != null) _setActive(v);
        }

        v = Pio.getValue(paramSet, COMPLETED_PROP);
        if (v != null) _setCompleted(v);
        v = Pio.getValue(paramSet, NOTIFY_PI_PROP);
        if (v != null) _setNotifyPi(v);

        v = Pio.getValue(paramSet, ROLLOVER_FLAG_PROP);
        if (v != null) setRolloverStatus(Boolean.parseBoolean(v));

        v = Pio.getValue(paramSet, THESIS_FLAG_PROP);
        if (v != null) setThesis(Boolean.parseBoolean(v));

        v = Pio.getValue(paramSet, LIBRARY_FLAG_PROP);
        if (v != null) setLibrary(Boolean.parseBoolean(v));

        // SCT-201
        ParamSet gsa = paramSet.getParamSet(GSA_PROP);
        if (gsa != null) _gsa = new GsaAspect(gsa);

        ParamSet gp1 = paramSet.getParamSet(GsaPhase1Data.PARAM_SET_NAME);
        if (gp1 != null) _gsaPhase1Data = new GsaPhase1Data(gp1);

        // UX-1520
//        _ignoredProblems = Pio.getValues(paramSet, IGNORED_PROBLEMS_PROP);
    }

    private static TimeValue parseTimeValueParam(ParamSet pset, String path) {
        Param timeParam = pset.getParam(path);
        if (timeParam == null) return null;

        String strVal = timeParam.getValue();
        double val;
        try {
            val = Double.parseDouble(strVal);
        } catch (Exception ex) {
            return null;
        }

        if (val <= 0) return null;

        TimeValue.Units units = TimeValue.Units.hours;
        String unitsStr = timeParam.getUnits();
        if (unitsStr != null) {
            try {
                units = TimeValue.Units.valueOf(unitsStr);
            } catch (Exception ex) {
                return null;
            }
        }

        return new TimeValue(val, units);
    }
}
