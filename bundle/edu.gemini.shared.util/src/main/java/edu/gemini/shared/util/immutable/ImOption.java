package edu.gemini.shared.util.immutable;

import scala.None$;
import scala.Option$;

import java.util.Optional;
import java.util.function.Supplier;

public final class ImOption {
    private ImOption() { }

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
        return optional.map(ImOption::apply).orElseGet(None::instance);
    }

    public static <T> scala.Option<T> toScalaOpt(final Option<T> opt) {
        return Option$.MODULE$.apply(opt.getOrNull());
    }

    public static <T> scala.Option<T> scalaNone() {
        return toScalaOpt(None.instance());
    }

    public static <T> Option<T> when(final boolean condition, Supplier<T> value) {
        return condition ? new Some<>(value.get()) : None.instance();
    }
}
