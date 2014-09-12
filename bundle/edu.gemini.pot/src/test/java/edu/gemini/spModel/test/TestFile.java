package edu.gemini.spModel.test;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TestFile {
    private static final Logger LOG = Logger.getLogger(TestFile.class.getName());

    private TestFile() {}

    // Finds the StackTraceElement of the client of TestFile so that the temp
    // file can contain the calling class and method in its name.
    private static StackTraceElement findCaller(StackTraceElement[] stack, int pos) {
        if (pos >= stack.length) throw new RuntimeException("Couldn't find caller!");
        final StackTraceElement candidate = stack[pos];

        final String className = candidate.getClassName();
        if ("java.lang.Thread".equals(className) || TestFile.class.getCanonicalName().equals(className)) {
            return findCaller(stack, ++pos);
        } else {
            return candidate;
        }
    }

    public static File create() {
        final StackTraceElement[] ste  = Thread.currentThread().getStackTrace();
        final StackTraceElement caller = findCaller(ste, 0);
        final String className         = caller.getClassName();
        final String methodName        = caller.getMethodName();
        final String fileName          = String.format("%s.%s", className, methodName);
        try {
            final File f = File.createTempFile(fileName, null);
            f.deleteOnExit();
            return f;
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't create temp file: " + fileName);
        }
    }

    public interface Arm<T> {
        public T apply(File f) throws Exception;
    }

    public static <T> T arm(Arm<T> a) throws Exception {
        final File f = create();
        try {
            return a.apply(f);
        } finally {
            if (f.exists() && !f.delete()) {
                LOG.log(Level.WARNING, "Couldn't delete test file: " + f);
            }
        }
    }

    /**
     * Serializes the given object out to a file then reads it back and returns
     * it.
     */
    @SuppressWarnings("unchecked")
    public static <T> T ser(final T tOut) throws Exception {
        return arm(new Arm<T>() {
            @Override
            public T apply(File f) throws Exception {
                final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
                try {
                    out.writeObject(tOut);
                    out.flush();
                } finally {
                    out.close();
                }

                final ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
                try {
                    return (T) in.readObject();
                } finally {
                    in.close();
                }
            }
        });
    }
}
