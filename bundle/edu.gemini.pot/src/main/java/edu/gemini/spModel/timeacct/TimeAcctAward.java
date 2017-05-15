package edu.gemini.spModel.timeacct;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;


/**
 * A tuple combining program and partner time awards.
 */
public final class TimeAcctAward implements Serializable {
    public static final TimeAcctAward ZERO = new TimeAcctAward(Duration.ZERO, Duration.ZERO);

    private static final long MS_PER_HOUR = Duration.ofHours(1).toMillis();

    private final Duration programAward;
    private final Duration partnerAward;

    public TimeAcctAward(Duration programAward, Duration partnerAward) {
        if (programAward == null)
            throw new IllegalArgumentException("null programAward in TimeAcctAward");

        if (partnerAward == null)
            throw new IllegalArgumentException("null partnerAward in TimeAcctAward");

        if (programAward.isNegative())
            throw new IllegalArgumentException("Negative programAward in TimeAcctAward");

        if (partnerAward.isNegative())
            throw new IllegalArgumentException("Negative partnerAward in TimeAcctAward");

        this.programAward = programAward;
        this.partnerAward = partnerAward;
    }

    private static double toHours(Duration d) {
        return ((double) d.toMillis()) / MS_PER_HOUR;
    }

    public Duration getProgramAward() {
        return programAward;
    }

    /** Convenience method to provide program award converted to hours. */
    public double getProgramHours() {
        return toHours(getProgramAward());
    }

    public Duration getPartnerAward() {
        return partnerAward;
    }

    /** Convenience method to provide partner award converted to hours. */
    public double getPartnerHours() {
        return toHours(getPartnerAward());
    }


    public Duration getTotalAward() {
        return programAward.plus(partnerAward);
    }

    /** Convenience method to provide total award converted to hours. */
    public double getTotalHours() {
        return toHours(getTotalAward());
    }

    public TimeAcctAward plus(TimeAcctAward that) {
        return new TimeAcctAward(programAward.plus(that.programAward), partnerAward.plus(that.partnerAward));
    }

    public boolean isZero() {
        return programAward.isZero() && partnerAward.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeAcctAward that = (TimeAcctAward) o;
        return Objects.equals(programAward, that.programAward) &&
                Objects.equals(partnerAward, that.partnerAward);
    }

    @Override
    public int hashCode() {
        return Objects.hash(programAward, partnerAward);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TimeAcctAward{");
        sb.append("programAward=").append(programAward);
        sb.append(", partnerAward=").append(partnerAward);
        sb.append('}');
        return sb.toString();
    }
}
