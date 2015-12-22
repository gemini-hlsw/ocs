package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.ISPRootNode;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MemSerializer {
    private static final Logger LOG = Logger.getLogger(MemSerializer.class.getName());

    // An ObjectInputStream that uses the bundle's class loader if possible to
    // resolve classes.
    private final class ClassLoaderObjectInputStream extends ObjectInputStream {
        private final ClassLoader loader;

        ClassLoaderObjectInputStream(ClassLoader loader, InputStream is) throws IOException {
            super(is);
            this.loader = loader;
        }

        @Override protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(osc.getName(), false, loader);
            } catch (ClassNotFoundException ex) {
               return super.resolveClass(osc);
            }
        }
    }

    private ClassLoader getLoader() {
        return MemSerializer.class.getClassLoader();
    }

    public MemAbstractBase load(File file) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        return loadAndClose(new ClassLoaderObjectInputStream(getLoader(), new BufferedInputStream(fis)));
    }

    public MemAbstractBase load(byte[] blob) throws IOException {
        return loadAndClose(new ClassLoaderObjectInputStream(getLoader(), new ByteArrayInputStream(blob)));
    }

    private MemAbstractBase loadAndClose(ObjectInputStream ois) throws IOException {
        try { return load(ois); } finally { ois.close(); }
    }

    public MemAbstractBase load(ObjectInputStream ois) throws IOException {
        try {
            return (MemAbstractBase) ois.readObject();
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.SEVERE, "Couldn't load program", ex);
            throw new RuntimeException(ex);
        }
    }

    public void store(ISPRootNode mab, File file) throws IOException {
        final FileOutputStream fos = new FileOutputStream(file);
        storeAndClose(mab, new ObjectOutputStream(new BufferedOutputStream(fos)));
    }

    public byte[] store(ISPRootNode mab) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        storeAndClose(mab, new ObjectOutputStream(baos));
        return baos.toByteArray();
    }

    private void storeAndClose(ISPRootNode mab, ObjectOutputStream oos) throws IOException {
        try { store(mab, oos); oos.flush(); } finally { oos.close(); }
    }

    public void store(ISPRootNode mab, final ObjectOutputStream oos) throws IOException {
        mab.getProgramReadLock();
        try {
            oos.writeObject(mab);
        } finally {
            mab.returnProgramReadLock();
        }
    }
}
