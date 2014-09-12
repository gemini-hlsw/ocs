package edu.gemini.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A disk-backed FIFO of arbitrary length. Multiple iterators may read from the FIFO,
 * and newly-added elements will be available immediately (i.e., you can read until the iterator
 * is empty, then add some elements and immediately read them from the same [now non-empty]
 * iterator). 
 * <p>
 * The backing store is a temporary file in the form <code>fifo-*.bin</code>, located in default
 * temporary storage. These files are deleted by the finalizer or VM exit, whichever happens first.
 * @param <T>
 */
public class ExternalFIFO<T extends Serializable> implements Iterable<T> {

	private static final Logger LOGGER = Logger.getLogger(ExternalFIFO.class.getName());
	private static final int BUFFER_SIZE = 1024 * 1024;	 // generous output buffer size
	private static final int RESET_INTERVAL = 10 * 1000; // reset output stream every 1000 objects

	// We need to run our own little GC here because our DGC interval is so long. We really do want
	// to finalize these guys after a while so we don't have files hanging around. For now we will
	// set a maximum lifetime of ten minutes ... this should be plenty. Yes, we should add close()
	// methods and require clients to call them, but we still need this check to be safe.
	private static final Set<ExternalFIFO> instances = new HashSet<ExternalFIFO>();
	private static final Timer timer = new Timer("FIFO Reaper", true);
	static {
		
		TimerTask reaper = new TimerTask() {		
			@Override
			public void run() {
				synchronized (ExternalFIFO.class) {
					long now = System.currentTimeMillis();
					for (Iterator<ExternalFIFO> it = instances.iterator(); it.hasNext();) {
						ExternalFIFO fifo = it.next();
						try {
							if (now >= fifo.endOfLife) {
								fifo.close();
								it.remove();
							}
						} catch (Throwable t) {
							LOGGER.log(Level.SEVERE, "Problem reaping FIFO on " + fifo.file, t);
						}
					}
				}
			}
		
		};
		
		timer.schedule(reaper, 0, 1000 * 15); // every 15 seconds (?)
		
	}

	private final long endOfLife = System.currentTimeMillis() + 1000 * 60 * 10; // 10 minutes
	private final File file;
	private final ObjectOutputStream out;
	
	private boolean open = true;
	private int count;
	
	// Keep a list of ClassLoaders. We will annotate each class with the index of its classloader
	// when the class descriptor is serialized, and lookup the classloader again when the class 
	// descriptor is deserialized. This is important because add() may be called in an RMI thread.
	private final List<ClassLoader> loaders = new ArrayList<ClassLoader>();
	
	public ExternalFIFO() {
		synchronized (ExternalFIFO.class) {
			instances.add(this);
		}
		try {
			file = File.createTempFile("fifo-", ".bin");
			file.deleteOnExit();
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE)) {
				@Override
				protected void annotateClass(Class<?> cl) throws IOException {
					super.annotateClass(cl); // does nothing, actually
					ClassLoader loader = cl.getClassLoader();
					int index = loaders.indexOf(loader);
					if (index == -1) {
						index = loaders.size();
						loaders.add(loader);						
						LOGGER.fine("loaders[" + index + "] => " + loader);
					}				
					writeInt(index);
				}
			};
			LOGGER.fine("Creating FIFO on " + file);
		} catch (IOException e) {
			String message = "Problem creating FIFO.";
			LOGGER.log(Level.SEVERE, message, e);
			throw new RuntimeException(message);
		}
	}	

	@Override
	protected void finalize() throws Throwable {
		try {
			close(true);
		} finally {
			super.finalize();
		}
	}

	public synchronized void close() {
		close(false);
	}
	
	private synchronized void close(boolean finalizing) {
		if (open) {
			LOGGER.fine("Closing FIFO on " + file);
			open = false;
			try {
				out.reset();
				out.close();
			} catch (IOException ioe) {
				if (!finalizing)
					LOGGER.log(Level.WARNING, "Trouble closing FIFO", ioe);
			}
			file.delete();
		}
	}
	
	public synchronized void add(T object) throws IOException {
		if (!open) throw new IllegalStateException("FIFO is closed.");
		if (object == null) throw new IllegalArgumentException("Can't add null.");
		synchronized (out) {
			// Must reset from time to time or the input stream will leak heap during read
			if (count % RESET_INTERVAL == 0)
				out.reset(); 
			out.writeUnshared(object);
		}
		count++; // atomic
	}

	public synchronized Iterator<T> iterator() {
		if (!open) throw new IllegalStateException("FIFO is closed.");
		try {
			return new StreamingIterator();
		} catch (IOException e) {
			String message = "Problem creating FIFO iterator on " + file;
			LOGGER.log(Level.SEVERE, message, e);
			throw new RuntimeException(message);
		}
	}

	
	
	
	private class StreamingIterator implements Iterator<T> {
		
		private int i = 0;
		private final ObjectInputStream in;
		
		public StreamingIterator() throws IOException {
			synchronized (out) {
				out.reset();
				out.flush();
			}
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					ClassLoader loader = loaders.get(readInt());
					return loader != null ? loader.loadClass(desc.getName()) : super.resolveClass(desc);
				}
			};
		}
		
		public boolean hasNext() {
			synchronized (ExternalFIFO.this) {
				return open && i < count;
			}
		}

		@SuppressWarnings("unchecked")
		public T next() {
			synchronized (ExternalFIFO.this) {
				if (i >= count || !open) throw new NoSuchElementException();
				++i;
				try {
					synchronized (out) {
						out.flush();
					}
					T ret = (T) in.readUnshared();
					return ret;
				} catch (IOException e) {
					String message = "Problem fetching next element from FIFO iterator on " + file;
					LOGGER.log(Level.SEVERE, message, e);
					throw new RuntimeException(message);
				} catch (ClassNotFoundException e) {
					String message = "Problem fetching next element from FIFO iterator on " + file;
					LOGGER.log(Level.SEVERE, message, e);
					throw new RuntimeException(message);
				}
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		protected void finalize() throws Throwable {
			in.close();
			super.finalize();
		}
		
	}
	
	
}
