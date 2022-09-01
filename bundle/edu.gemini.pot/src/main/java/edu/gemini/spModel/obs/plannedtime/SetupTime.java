package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

/**
 * SetupTime combines the time required for two distinct methods of determining
 * the setup time along with a user preference for which one to use.  The
 * preference is kept with the observation node itself in SPObservation while
 * the setup time options are found in the individual instrument components.
 */
public final class SetupTime implements Serializable {

    /**
     * Which type of setup is expected.
     */
    public enum Type {
        FULL,
        REACQUISITION,
        NONE
        ;
    }

    /** Full setup time including initial acquisition. */
    public final Duration fullSetupTime;

    /** Setup time when only reacquisition is needed. */
    public final Duration reacquisitionOnlyTime;

    /** Which type of acquisition is expected. */
    public final Type acquisitionType;

    private SetupTime(Duration full, Duration reacquisition, Type setupType) {
        assert !full.isNegative();
        assert !reacquisition.isNegative();

        System.out.println("SetupTimeeeeee "+ full.getSeconds() + "  reacquisitionOnlyTime: " + reacquisition.getSeconds() + "  setupType: "+ setupType.name());
        this.fullSetupTime         = full;
        this.reacquisitionOnlyTime = reacquisition;
        this.acquisitionType       = setupType;
    }

    /**
     * Extracts the time required for the setup corresponding to the setup time
     * type.
     */
    public Duration toDuration() {
        final Duration result;
        switch (acquisitionType) {
            case FULL:          result = fullSetupTime;         break;
            case REACQUISITION: result = reacquisitionOnlyTime; break;
            case NONE:          result = Duration.ZERO;         break;
            default:
                throw new IllegalArgumentException();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetupTime that = (SetupTime) o;
        return Objects.equals(fullSetupTime, that.fullSetupTime) &&
                Objects.equals(reacquisitionOnlyTime, that.reacquisitionOnlyTime) &&
                Objects.equals(acquisitionType, that.acquisitionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullSetupTime, reacquisitionOnlyTime, acquisitionType);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SetupTime{");
        sb.append("fullSetupTime=").append(fullSetupTime);
        sb.append(", reacquisitionOnlyTime=").append(reacquisitionOnlyTime);
        sb.append(", acquisitionType=").append(acquisitionType);
        sb.append('}');
        return sb.toString();
    }

    public static SetupTime ZERO =
        SetupTime.unsafeFromDuration(Duration.ZERO, Duration.ZERO, Type.FULL);

    private static Duration duration(double secs) {
        return Duration.ofMillis(Math.round(secs * 1000.0));
    }

    public static Option<SetupTime> fromSeconds(double full, double reacquisition, Type type) {
        return ((full < 0.0) || (reacquisition < 0.0)) ?
            ImOption.<SetupTime>empty()                :
            new Some<>(new SetupTime(duration(full), duration(reacquisition), type));
    }

    public static Option<SetupTime> fromDuration(Duration full, Duration reacquisition, Type type) {
        return (full.isNegative() || reacquisition.isNegative()) ?
           ImOption.<SetupTime>empty()                           :
           new Some<>(new SetupTime(full, reacquisition, type));
    }

    public static SetupTime unsafeFromSeconds(double full, double reacquisition, Type type) {
        return fromSeconds(full, reacquisition, type).getValue();
    }

    public static SetupTime unsafeFromDuration(Duration full, Duration reacquisition, Type type) {
        return fromDuration(full, reacquisition, type).getValue();
    }

}
