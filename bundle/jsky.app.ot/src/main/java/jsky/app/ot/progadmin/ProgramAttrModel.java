//
// $
//

package jsky.app.ot.progadmin;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.Serializable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administration properties that apply to a science program as a whole.
 */
final class ProgramAttrModel implements Serializable {
    private static final Logger LOG = Logger.getLogger(ProgramAttrModel.class.getName());

    private static final List<InternetAddress> EMPTY_ADDRESS_LIST = Collections.emptyList();

    static final class Builder implements Serializable {
        private SPProgramID progId;
        private SPProgram.ProgramMode mode = SPProgram.ProgramMode.DEFAULT;
        private Integer queueBand;
        private List<InternetAddress> gemContactEmails = EMPTY_ADDRESS_LIST;
        private Affiliate affiliate;
        private TooType tooType;
        private boolean rollover;
        private boolean thesis;
        private boolean library;

        Builder programId(SPProgramID progId) {
            this.progId = progId;
            return this;
        }

        Builder programMode(SPProgram.ProgramMode mode) {
            this.mode = mode;
            return this;
        }

        Builder queueBand(Integer band) {
            this.queueBand = band;
            return this;
        }

        Builder geminiContactEmails(Collection<InternetAddress> emails) {
            if (emails == null) {
                this.gemContactEmails = EMPTY_ADDRESS_LIST;
            } else {
                this.gemContactEmails = new ArrayList<InternetAddress>(emails);
            }
            return this;
        }

        Builder affiliate(Affiliate affiliate) {
            this.affiliate = affiliate;
            return this;
        }

        Builder tooType(TooType tooType) {
            this.tooType = tooType;
            return this;
        }

        Builder rollover(boolean rollover) {
            this.rollover = rollover;
            return this;
        }

        Builder isThesis(boolean thesis) {
            this.thesis = thesis;
            return this;
        }

        Builder isLibrary(boolean library) {
            this.library = library;
            return this;
        }

        ProgramAttrModel build() {
            return new ProgramAttrModel(this);
        }
    }

    private final SPProgramID progId;
    private final SPProgram.ProgramMode mode;
    private final Integer queueBand;

    private final List<InternetAddress> gemContactEmails;

    // program affiliate, which is stored as a string :-( and kept in the
    // SPProgram.PIInfo :-(
    private final Affiliate affiliate;

    private final TooType tooType;

    private final boolean rollover;
    private final boolean thesis;
    private final boolean library;

    private ProgramAttrModel(Builder b)  {
        this.progId    = b.progId;
        this.mode      = b.mode;
        this.queueBand = b.queueBand;
        this.gemContactEmails = b.gemContactEmails;
        this.affiliate = b.affiliate;
        this.tooType   = b.tooType;
        this.rollover  = b.rollover;
        this.thesis    = b.thesis;
        this.library   = b.library;
    }

    ProgramAttrModel(ISPProgram prog)  {
        progId = prog.getProgramID();

        SPProgram dataObj = (SPProgram) prog.getDataObject();

        SPProgram.ProgramMode mode;
        mode = dataObj.getProgramMode();
        if (mode == null) mode = SPProgram.ProgramMode.DEFAULT;
        this.mode = mode;

        Integer queueBand = null;
        String bandStr = dataObj.getQueueBand();
        if (bandStr != null) {
            try {
                Integer bandInt = Integer.parseInt(bandStr);
                if ((bandInt != null) && (bandInt > 0)) queueBand = bandInt;
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        this.queueBand = queueBand;

        String emailStr = dataObj.getContactPerson();
        gemContactEmails = parseAddresses(emailStr);

        SPProgram.PIInfo piInfo = dataObj.getPIInfo();
        Affiliate affiliate = null;
        if (piInfo != null) {
            affiliate = piInfo.getAffiliate();
        }
        this.affiliate = affiliate;
        this.tooType = Too.get(prog);

        this.rollover = dataObj.getRolloverStatus();
        this.thesis   = dataObj.isThesis();
        this.library  = dataObj.isLibrary();
    }


    static List<InternetAddress> parseAddresses(String addrListStr) {
        if ((addrListStr == null) || "".equals(addrListStr.trim())) {
            return EMPTY_ADDRESS_LIST;
        }

        List<InternetAddress> res = new ArrayList<InternetAddress>();
        StringTokenizer st = new StringTokenizer(addrListStr, " \t,;", false);

        while (st.hasMoreTokens()) {
            String addrStr = st.nextToken();

            try {
                InternetAddress ia = new InternetAddress(addrStr);
                ia.validate();
                res.add(ia);
            } catch (AddressException ex) {
                String msg = "Invalid address: " + addrStr;
                LOG.log(Level.WARNING, msg);
            }
        }

        return res;
    }

    static String emailsToString(List<InternetAddress> addressList) {
        StringBuilder buf = new StringBuilder();
        if (addressList.size() > 0) {
            buf.append(addressList.get(0));
            for (InternetAddress ia : addressList.subList(1, addressList.size())) {
                buf.append(", ").append(ia);
            }
        }
        return buf.toString();
    }

    public void apply(SPProgram dataObj) {
        dataObj.setProgramMode(mode);
        dataObj.setQueueBand((queueBand == null) ? "" : queueBand.toString());
        dataObj.setContactPerson(emailsToString(gemContactEmails));
        dataObj.setRolloverStatus(rollover);
        dataObj.setThesis(thesis);
        dataObj.setLibrary(library);

        // grim grim
        SPProgram.PIInfo piInfo = dataObj.getPIInfo();
        dataObj.setPIInfo(piInfo.withAffiliate(affiliate));
    }

    public SPProgramID getProgramId() {
        return progId;
    }

    public Option<ProgramType> getProgramType() {
        return ImOption.apply(ProgramType$.MODULE$.readOrNull(progId));
    }

    public SPProgram.ProgramMode getProgramMode() {
        return mode;
    }

    public Integer getQueueBand() {
        return queueBand;
    }

    public boolean isRollover() {
        return rollover;
    }

    public TooType getTooType() {
        return tooType;
    }

    public boolean isThesis() {
        return thesis;
    }

    public boolean isLibrary() {
        return library;
    }

    public List<InternetAddress> getGeminiContactEmails() {
        return new ArrayList<InternetAddress>(gemContactEmails);
    }

    public Affiliate getAffiliate() {
        return affiliate;
    }

    /*
    public Builder builder() {
        Builder b = new Builder();
        b.programId(progId);
        b.programMode(mode);
        b.queueBand(queueBand);
        b.rollover(rollover);
        b.tooType(tooType);
        b.isThesis(thesis);
        b.geminiContactEmails(gemContactEmails);
        b.affiliate(affiliate);
        return b;
    }
    */
}
