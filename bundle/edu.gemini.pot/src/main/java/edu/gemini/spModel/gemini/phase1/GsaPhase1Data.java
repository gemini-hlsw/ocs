package edu.gemini.spModel.gemini.phase1;

import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class contains Phase 1 data that isn't used in the OCS or OT but is
 * required by the GSA.  This class was introduced for the 2012B semester to
 * track required GSA information since the old Phase1Data node is no
 * longer being stored.
 *
 * <p>The TAC category and keywords are stored as Strings instead of enums
 * because</p>
 *
 * <ul>
 * <li>they are never used for anything other than sending the value to the GSA</li>
 * <li>they are defined in the ocs2 phase 1 model, which cannot be directly included in the spModel</li>
 * <li>making a copy of the 100+ keywords for use in the spModel and keeping it in sync is unappealing</li>
 * </ul>
 */
public final class GsaPhase1Data implements Serializable {

    public static final String PARAM_SET_NAME          = "phase1";
    public static final String ABSTRACT_PARAM          = "abstract";
    public static final String CATEGORY_PARAM          = "category";
    public static final String KEYWORD_PARAM           = "keyword";
    public static final String INVESTIGATORS_PARAM_SET = "investigators";
    public static final String PI_PARAM_SET            = "pi";
    public static final String COI_PARAM_SET           = "coi";

    public static final GsaPhase1Data EMPTY = new GsaPhase1Data(
            Abstract.EMPTY,
            Category.EMPTY,
            Investigator.EMPTY,
            Collections.emptyList()
    );

    /** Just an interface to give a type to an otherwise raw String value. */
    public interface StringType {
        String getValue();
    }

    static class StringWrapper implements StringType, Serializable {
        private final String value;

        protected StringWrapper(String value) {
            if (value == null) value = "";
            this.value = value;
        }

        @Override public String getValue() { return value; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringWrapper that = (StringWrapper) o;

            if (!value.equals(that.value)) return false;

            return true;
        }

        @Override public int hashCode() {
            return value.hashCode();
        }

        @Override public String toString() { return value; }
    }

    public static final class Abstract extends StringWrapper {
        public static final Abstract EMPTY = new Abstract("");
        public Abstract(String value) { super(value); }
    }
    public static final class Category extends StringWrapper {
        public static final Category EMPTY = new Category("");
        public Category(String value) { super(value); }
    }
    public static final class Keyword extends StringWrapper {
        public static final Keyword EMPTY = new Keyword("");
        public Keyword(String value) { super(value); }
    }

    public static final class Investigator implements Serializable {
        public static final String FIRST_PARAM = "first";
        public static final String LAST_PARAM  = "last";
        public static final String EMAIL_PARAM = "email";

        public static final Investigator EMPTY = new Investigator("", "", "");

        private final String first;
        private final String last;
        private final String email;

        public Investigator(String first, String last, String email) {
            if (first == null) first = "";
            if (last  == null) last  = "";
            if (email == null) email = "";

            this.first = first;
            this.last  = last;
            this.email = email;
        }

        public Investigator(ParamSet pset) {
            this.first = Pio.getValue(pset, FIRST_PARAM, "");
            this.last  = Pio.getValue(pset, LAST_PARAM,  "");
            this.email = Pio.getValue(pset, EMAIL_PARAM, "");
        }

        public ParamSet toParamSet(PioFactory factory, String name) {
            ParamSet pset = factory.createParamSet(name);
            Pio.addParam(factory, pset, FIRST_PARAM, first);
            Pio.addParam(factory, pset, LAST_PARAM,  last);
            Pio.addParam(factory, pset, EMAIL_PARAM, email);
            return pset;
        }

        public String getFirst() { return first; }
        public String getLast()  { return last;  }
        public String getEmail() { return email; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Investigator that = (Investigator) o;
            if (!email.equals(that.email)) return false;
            if (!first.equals(that.first)) return false;
            if (!last.equals(that.last)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = first.hashCode();
            result = 31 * result + last.hashCode();
            result = 31 * result + email.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Investigator");
            sb.append("{first='").append(first).append('\'');
            sb.append(", last='").append(last).append('\'');
            sb.append(", email='").append(email).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private final String abstrakt;
    private final String category;
    private final Investigator pi;
    private final List<Investigator> cois;

    public GsaPhase1Data(Abstract abstrakt, Category cat, Investigator pi, Collection<Investigator> cois) {
        if (abstrakt == null) abstrakt = new Abstract("");
        if (cat == null) cat = new Category("");
        if (pi == null) pi = Investigator.EMPTY;
        if (cois == null) cois = Collections.emptyList();

        this.abstrakt = abstrakt.getValue();
        this.category = cat.getValue();
        this.pi = pi;
        if (cois.size() == 0) {
            this.cois = Collections.emptyList();
        } else {
            List<Investigator> is = new ArrayList<>(cois.size());
            is.addAll(cois);
            this.cois = Collections.unmodifiableList(is);
        }
    }

    public GsaPhase1Data(ParamSet pset) {
        this.abstrakt = Pio.getValue(pset, ABSTRACT_PARAM, "");
        this.category = Pio.getValue(pset, CATEGORY_PARAM, "");

        List<Param> ks = pset.getParams(KEYWORD_PARAM);

        ParamSet invsPset = pset.getParamSet(INVESTIGATORS_PARAM_SET);
        if (invsPset == null) {
            this.pi   = Investigator.EMPTY;
            this.cois = Collections.emptyList();
        } else {
            ParamSet piPset = invsPset.getParamSet(PI_PARAM_SET);
            if (piPset == null) {
                this.pi = Investigator.EMPTY;
            } else {
                this.pi = new Investigator(piPset);
            }
            List<ParamSet> coisPset = invsPset.getParamSets(COI_PARAM_SET);
            if (coisPset.size() == 0) {
                this.cois = Collections.emptyList();
            } else {
                List<Investigator> cois = new ArrayList<>(coisPset.size());
                for (ParamSet coiPset : coisPset) {
                    cois.add(new Investigator(coiPset));
                }
                this.cois = Collections.unmodifiableList(cois);
            }
        }
    }

    public Abstract getAbstract() { return new Abstract(abstrakt); }
    public Category getCategory() { return new Category(category); }
    public Investigator getPi() { return pi; }
    public List<Investigator> getCois() { return cois; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(PARAM_SET_NAME);
        Pio.addParam(factory, pset, ABSTRACT_PARAM, abstrakt);
        Pio.addParam(factory, pset, CATEGORY_PARAM, category);

        ParamSet invPset = factory.createParamSet(INVESTIGATORS_PARAM_SET);
        invPset.addParamSet(pi.toParamSet(factory, PI_PARAM_SET));
        for (Investigator coi : cois) {
            invPset.addParamSet(coi.toParamSet(factory, COI_PARAM_SET));
        }
        pset.addParamSet(invPset);
        return pset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GsaPhase1Data that = (GsaPhase1Data) o;

        if (!abstrakt.equals(that.abstrakt)) return false;
        if (!category.equals(that.category)) return false;
        if (!pi.equals(that.pi)) return false;
        if (!cois.equals(that.cois)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = abstrakt.hashCode();
        result = 31 * result + category.hashCode();
        result = 31 * result + pi.hashCode();
        result = 31 * result + cois.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GsaPhase1Data");
        sb.append("{abstrakt='").append(abstrakt).append('\'');
        sb.append(", category='").append(category).append('\'');
        sb.append(", pi=").append(pi);
        sb.append(", cois=").append(cois);
        sb.append('}');
        return sb.toString();
    }
}
