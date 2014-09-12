package edu.gemini.qpt.shared.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.gemini.spModel.core.SPProgramID;

/**
 * SPProgramID that knows its internal structure.
 * Note: This code can probably be replaced with ProgramId / ProgramType from the core module.
 * @author rnorris
 */
public class StructuredProgramID implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Pattern SEMESTER_PATTERN =
		Pattern.compile("(G[NS])-(\\d{4}+[AB])-([A-Z]+)-(\\d+)");
    private static final Pattern ENGINEERING_PATTERN = Pattern.compile("(G[NS])-ENG(((\\d{4})(\\d{2})(\\d{2}))|-(.+))");
    private static final Pattern CALIBRATION_PATTERN = Pattern.compile("(G[NS])-CAL(((\\d{4})(\\d{2})(\\d{2}))|-(.+))");

	private final String site;
	private final String semester;
	private final Type type;
	private final int number;
    private final String suffix;

    /** TODO: Switch to edu.gemini.spModel.core.ProgramId? */
	public enum Type {
        LP("Large Programs"),
        FT("Fast Turnaround"),
		Q("Queue"),
		C("Classical"),
		SV("Science Verification"),
		DD("Director's Discretion"),
		DS("Demonstration Science"),
        ENG("Engineering"),
        CAL("Calibration")
		;
		private final String description;
		private Type(String description) {
			this.description = description;
		}
		public String getDescription() {
			return description + " (" + name() + ")";
		}
	}

	public StructuredProgramID(SPProgramID id) {
		this(id.stringValue());
	}

	public StructuredProgramID(String id) {
        Matcher m = ENGINEERING_PATTERN.matcher(id);
        if (m.matches()) {
            this.site = m.group(1);
            if ( m.group(3) != null){
                int month = Integer.parseInt(m.group(5));
                if (month<2) {
                    this.semester = String.valueOf(Integer.parseInt(m.group(4)) - 1) + "B";
                } else if (month<=7){
                    this.semester = m.group(4) + "A";
                } else {
                    this.semester = m.group(4) + "B";
                }
                this.suffix = m.group(5) + m.group(6);
            } else {
                this.suffix = m.group(7);
                this.semester = null;
            }
            this.type = Type.ENG;
            this.number = 0;
        } else {
            m = CALIBRATION_PATTERN.matcher(id);
            if (m.matches()) {
                this.site = m.group(1);
                if ( m.group(3) != null){
                    int month = Integer.parseInt(m.group(5));
                    if (month<2) {
                        this.semester = String.valueOf(Integer.parseInt(m.group(4)) - 1) + "B";
                    } else if (month<=7){
                        this.semester = m.group(4) + "A";
                    } else {
                        this.semester = m.group(4) + "B";
                    }
                    this.suffix = m.group(5) + m.group(6);
                } else {
                    this.suffix = m.group(7);
                    this.semester = null;
                }
                this.type = Type.CAL;
                this.number = 0;
            } else {
                m = SEMESTER_PATTERN.matcher(id);
                if (!m.matches()) throw new IllegalArgumentException("Nonstandard program ID.");
                this.site = m.group(1);
                this.semester = m.group(2);
                this.type = Type.valueOf(m.group(3));
                this.number = Integer.parseInt(m.group(4));
                this.suffix = null;
            }
        }
	}

	@Override
	public String toString() {
        if (type == Type.ENG || type == Type.CAL) {
            if ( semester == null ) {
                return site + "-" + type + suffix;
            } else {
                return site + "-" + semester + "-" + type + "-" + suffix;
            }
        } else {
		    return site + "-" + semester + "-" + type + "-" + number;
        }
	}

	public String getShortName() {
        if (type == Type.ENG || type == Type.CAL) {
            if ( semester == null ) {
                return site.charAt(1) + "-" + type + "-" + suffix;
            } else {
                return site.charAt(1) + semester.substring(2) + "-" + type + "-" + suffix;
            }
        } else {
		    return site.charAt(1) + semester.substring(2) + "-" + type + "-" + number;
        }
	}

	public String getSemester() {
		return semester;
	}

	public Type getType() {
		return type;
	}

    public int getNumber() {
        return number;
    }

}
