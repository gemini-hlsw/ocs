package edu.gemini.shared.util.immutable;

/**
 * Class ImOption
 *
 * @author Nicolas A. Barriga
 *         Date: 5/3/12
 */
public class ImOption {
    public static <T> Option<T> apply(T value) {
        if (value == null) {
            return None.instance();
        } else {
            return new Some(value);
        }
    }
}
