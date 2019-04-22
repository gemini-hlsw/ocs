package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.time.Duration;
import java.util.Objects;


public final class SetupTime {

    public enum Type {
        FULL,
        REACQUISITION,
        NONE
        ;
    }

    public final Duration fullSetupTime;
    public final Duration reacquisitionOnlyTime;
    public final Type acquisitionType;

    private SetupTime(Duration full, Duration reacquisition, Type setupType) {
        assert !full.isNegative();
        assert !reacquisition.isNegative();

        this.fullSetupTime         = full;
        this.reacquisitionOnlyTime = reacquisition;
        this.acquisitionType       = setupType;
    }

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
