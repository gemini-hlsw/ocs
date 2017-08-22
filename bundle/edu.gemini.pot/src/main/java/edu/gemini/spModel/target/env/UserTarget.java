package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.SPTarget;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Tuple of a user SPTarget and its type.
 */
public final class UserTarget implements Serializable {

    /**
     * Enumeration of the usages of additional non-guide star targets in a
     * target environment.
     */
    public enum Type {
        blindOffset("Blind-offset"),
        offAxis("Off-axis"),
        tuning("Tuning Star"),
        other("User");

        public final String displayName;

        private Type(String displayName) {
            this.displayName = displayName;
        }

        public static Option<Type> fromString(String s) {
            return ImOption.fromOptional(
                    Stream.of(values())
                            .filter(t -> t.name().equals(s))
                            .findFirst()
            );
        }
    }

    public final Type type;
    public final SPTarget target;

    public UserTarget(Type type, SPTarget target) {
        if (type == null) throw new NullPointerException("type cannot be null");
        if (target == null) throw new NullPointerException("target cannot be null");

        this.type   = type;
        this.target = target;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTarget that = (UserTarget) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, target);
    }

    public UserTarget cloneTarget() {
        return new UserTarget(type, target.clone());
    }
}
