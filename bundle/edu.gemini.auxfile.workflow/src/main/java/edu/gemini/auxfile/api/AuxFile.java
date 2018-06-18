//
// $Id: AuxFile.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.api;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.Option;

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;

/**
 * Description of an auxiliary file associated with a science program.
 */
public final class AuxFile implements Serializable, Comparable<AuxFile> {

    /**
     * Compares two AuxFiles based primarily upon the combination of program id
     * and file name.  If this comparison does not distinguish the two
     * instances, then last modification time, size, description, and checked status
     * are compared in that order.  This comparator is consistent with
     * <code>equals()</code>.
     *
     * <p>The AuxFile <code>compareTo()</code> is defined in terms of this
     * Comparator.
     */
    public static final class NameComparator implements Comparator<AuxFile> {
        public int compareIdsAndNames(AuxFile af1, AuxFile af2) {
            // Compare program ids.
            int res = af1.getProgramId().compareTo(af2.getProgramId());
            if (res != 0) return res;

            // Compare file names.
            return af1.getName().compareTo(af2.getName());
        }

        public int compareLastModTimes(AuxFile af1, AuxFile af2) {
            long af1Mod = af1.getLastModified();
            long af2Mod = af2.getLastModified();
            if (af1Mod < af2Mod) {
                return -1;
            } else if (af1Mod > af2Mod) {
                return 1;
            }
            return 0;
        }

        public int compareSizes(AuxFile af1, AuxFile af2) {
            long af1Size = af1.getSize();
            long af2Size = af2.getSize();
            if (af1Size < af2Size) {
                return -1;
            } else if (af1Size > af2Size) {
                return 1;
            }
            return 0;
        }

        public int compareDescriptions(AuxFile af1, AuxFile af2) {
            String af1Desc = af1.getDescription();
            String af2Desc = af2.getDescription();
            if (af1Desc == null) {
                return (af2Desc == null) ? 0 : -1;
            } else if (af2Desc == null) {
                return 1;
            }
            return af1Desc.compareTo(af2Desc);
        }

        public int compareChecked(AuxFile af1, AuxFile af2) {
            return (af2.isChecked() == af1.isChecked() ? 0 : (af1.isChecked() ? 1 : -1));
        }

        public int compareLastEmailed(AuxFile af1, AuxFile af2) {
            final Instant i1 = af1.getLastEmailed().getOrElse(Instant.MIN);
            final Instant i2 = af2.getLastEmailed().getOrElse(Instant.MIN);
            return i1.compareTo(i2);
        }

        public int compare(AuxFile af1, AuxFile af2) {
            int res = compareIdsAndNames(af1, af2);
            if (res != 0) return res;

            res = compareLastModTimes(af1, af2);
            if (res != 0) return res;

            res = compareSizes(af1, af2);
            if (res != 0) return res;

            res = compareDescriptions(af1, af2);
            if (res != 0) return res;

            res = compareChecked(af1, af2);
            if (res != 0) return res;

            return compareLastEmailed(af1, af2);
        }
    }

    public static final NameComparator NAME_COMPARATOR = new NameComparator();

    /**
     * Compares two AuxFile implementations based primarily upon their last
     * modification times.  If this fails to distinguish the two instances, then
     * the result will be the same as the {@link #NAME_COMPARATOR}.
     */
    public static final Comparator<AuxFile> LAST_MOD_TIME_COMPARATOR = new Comparator<AuxFile>() {
        public int compare(AuxFile af1, AuxFile af2) {
            int res = NAME_COMPARATOR.compareLastModTimes(af1, af2);
            if (res != 0) return res;

            return NAME_COMPARATOR.compare(af1, af2);
        }
    };

    private final SPProgramID pid;
    private final String name;
    private final String description;
    private final long size;
    private final long lastMod;
    private final boolean checked;
    private final Option<Instant> lastEmailed;

    public AuxFile(
            SPProgramID progId,
            String name,
            String description,
            long size,
            long lastMod,
            boolean checked,
            Option<Instant> lastEmailed
    ) {
        if (progId == null) throw new NullPointerException("missing progID");
        if (name == null) throw new NullPointerException("missing name");
        if (size < 0) throw new IllegalArgumentException("negative file size: " + size);
        if (lastMod < 0) throw new IllegalArgumentException("negative lastMod: " + lastMod);
        if (lastEmailed == null) throw new NullPointerException("missing lastEmailed");

        this.pid         = progId;
        this.name        = name;
        this.description = description;
        this.size        = size;
        this.lastMod     = lastMod;
        this.checked     = checked;
        this.lastEmailed = lastEmailed;
    }

    public AuxFile(
            SPProgramID progId,
            File file,
            String description,
            boolean checked,
            Option<Instant> lastEmailed
    ) {
        if (progId == null) throw new NullPointerException("missing progID");
        if (lastEmailed == null) throw new NullPointerException("missing lastEmailed");

        this.pid         = progId;
        this.name        = file.getName();
        this.description = description;
        this.size        = file.length();
        this.lastMod     = file.lastModified();
        this.checked     = checked;
        this.lastEmailed = lastEmailed;
    }

    /**
     * The id of the associated program.
     *
     * @return SPProgramID of the associated program
     */
    public SPProgramID getProgramId() { return pid; }

    /**
     * Gets the name of the file.
     *
     * @return name of the file
     */
    public String getName() { return name; }

    /**
     * Gets an (optional) description of the file.
     *
     * @return description associated with the file
     */
    public String getDescription() { return description; }

    /**
     * Gets the size of the file in bytes.
     *
     * @return file size in bytes
     */
    public long getSize() { return size; }

    /**
     * Gets the timestamp of the last modification made to the file.
     *
     * @return timestamp of the last file modification
     */
    public long getLastModified() { return lastMod; }

    /**
     * Returns true if this auxfile has been checked.
     */
    public boolean isChecked() { return checked; }

    /**
     * Returns the time at which a warning email was last sent, if any.
     */
    public Option<Instant> getLastEmailed() { return lastEmailed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuxFile auxFile = (AuxFile) o;

        if (!lastEmailed.equals(auxFile.lastEmailed)) return false;
        if (checked != auxFile.checked) return false;
        if (lastMod != auxFile.lastMod) return false;
        if (size != auxFile.size) return false;
        if (description != null ? !description.equals(auxFile.description) : auxFile.description != null)
            return false;
        if (!name.equals(auxFile.name)) return false;
        if (!pid.equals(auxFile.pid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pid.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (lastMod ^ (lastMod >>> 32));
        result = 31 * result + (checked ? 1 : 0);
        result = 31 * result + lastEmailed.hashCode();
        return result;
    }

    /**
     * Compares this AuxFile to another based upon the outcome of the
     * {@link #NAME_COMPARATOR} defined in this interface.
     */
    @Override public int compareTo(AuxFile that) {
        return NAME_COMPARATOR.compare(this, that);
    }

    @Override public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(pid.toString());
        buf.append(' ');
        buf.append(getName());
        buf.append(' ');
        buf.append(getSize());
        buf.append(' ');
        buf.append(new Date(getLastModified()));
        if (getDescription() != null) {
            buf.append(" [").append(getDescription()).append(']');
        }
        buf.append(" [checked=" + isChecked() + "]");
        buf.append(" [lastEmailed=" + lastEmailed.map(i -> DateTimeFormatter.ISO_INSTANT.format(i)).getOrElse("") + "]");
        return buf.toString();
    }
}
