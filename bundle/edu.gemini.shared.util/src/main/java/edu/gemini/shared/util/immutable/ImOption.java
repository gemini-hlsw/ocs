package edu.gemini.shared.util.immutable;

public class ImOption {
    public static <T> Option<T> apply(final T value) {
        if (value == null) {
            return None.instance();
        } else {
            return new Some<>(value);
        }
    }
}
