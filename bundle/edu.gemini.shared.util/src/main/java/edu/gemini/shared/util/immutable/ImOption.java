package edu.gemini.shared.util.immutable;

import java.util.Optional;

public class ImOption {
    public static <T> Option<T> apply(final T value) {
        if (value == null) {
            return None.instance();
        } else {
            return new Some<>(value);
        }
    }

    public static <T> Option<T> empty() {
        return None.instance();
    }

    public static <T> Option<T> fromScalaOpt(final scala.Option<T> scalaOpt) {
        return scalaOpt.isDefined() ? new Some<>(scalaOpt.get()) : None.instance();
    }

    public static <T> Option<T> fromOptional(final Optional<T> optional) {
        return optional.isPresent() ? new Some<>(optional.get()) : None.instance();
    }
}
