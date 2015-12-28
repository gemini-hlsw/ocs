package edu.gemini.shared.util.immutable;

public class ImOption {
    public static <T> Option<T> apply(final T value) {
        if (value == null) {
            return None.instance();
        } else {
            return new Some<>(value);
        }
    }

    public static <T> Option<T> fromScalaOpt(final scala.Option<T> scalaOpt) {
        return scalaOpt.isDefined() ? new Some<>(scalaOpt.get()) : None.instance();
    }

    public static <T> scala.Option<T> toScalaOpt(final Option<T> geminiOpt) {
        return scala.Option.apply(geminiOpt.getOrNull());
    }
}
