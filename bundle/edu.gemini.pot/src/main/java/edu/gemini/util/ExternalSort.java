package edu.gemini.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class ExternalSort {
	
	private static final Logger LOGGER = Logger.getLogger(ExternalSort.class.getName());
	private static int tapes; // must be >= 2
	private static int INITIAL; // shuold be divisible by TAPES
	
	static {
		// 8 tapes is pretty good for several hundred to several tens of thousands of elements.
		// After that you might want to experiment with more. Do benchmarking; it's not always
		// obvious. Sometimes odd numbers do better.
		setTapes(8);
	}
	
	public static int getTapes() {
		return tapes;
	}

	public static void setTapes(int tapes) {
		if (tapes < 2)
			throw new IllegalArgumentException("Tapes must be at least 2.");
		ExternalSort.tapes = tapes;
		INITIAL = tapes * 512;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> ExternalFIFO<T> sort(Iterable<T> source, Comparator<T> comparator) throws IOException {
		
		// Ok, we need two arrays of FIFOs, one for reading and one for writing
		ExternalFIFO<T>[] in = null, out = newOutputArray();
		
		// Load up the first set of FIFOs with our original data in fairly large blocks so we can
		// skip the early inefficient passes.
		List<T> set = new ArrayList<T>(INITIAL);
		for (T t: source) {
			set.add(t);
			if (set.size() == INITIAL) write(set, out, comparator);
		}
		write(set, out, comparator);
		
		// Ok, do the external merge, closing the input FIFOs after each pass and turning the output
		// FIFOs into the new input FIFOs. Back and forth until the merge produces just 1 block.
		for (int blockSize = INITIAL / tapes; ; blockSize *= 2) {
			in = out;
			out = newOutputArray();
			try {
				if (merge(in, out, comparator, blockSize) == 1) break;
			} finally {
				for (ExternalFIFO<T> f: in) f.close();				
			}
		}
		
		// Close all but the first output, returning it as the result.
		for (int i = 1; i < out.length; i++) out[i].close();		
		return out[0];
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Serializable & Comparable<T>> ExternalFIFO<T> parallelSort(Iterable<T> source, int threads) throws IOException {
		return parallelSort(source, new Comparator<T>() {
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		}, threads);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> ExternalFIFO<T> parallelSort(Iterable<T> source, final Comparator<T> comparator, int threads) throws IOException {

		// Ok, we're going to bust the source into an array of sources, then sort them on separate
		// threads, then merge them back again. This may be faster because so much time is spent in
		// blocking I/O.
		final ExternalFIFO<T>[] sources = new ExternalFIFO[threads];
		for (int i = 0; i < sources.length; i++) sources[i] = new ExternalFIFO<T>();
		int size = 0;
		for (T t: source)
			sources[size++ % threads].add(t);
		
		// Create and start the workers		
		Thread[] workers = new Thread[threads];
		final ExternalFIFO<T>[] results = new ExternalFIFO[threads];
		for (int i = 0; i < workers.length; i++) {
			final int _i = i;
			workers[i] = new Thread(new Runnable() {
				public void run() {
					try {
//						LOGGER.info("Starting thread " + _i);
						results[_i] = sort(sources[_i], comparator);
//						LOGGER.info("Exiting thread " + _i);
					} catch (IOException e) {
						// TODO: handle this
						e.printStackTrace();
					}
				}
			});
			workers[i].start();
		}
		
		// Wait for them all
		for (Thread t: workers) {
			for (;;) {
				try {
					t.join();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

//		for (ExternalFIFO<T> fifo: results) {
//			System.out.print("FIFO: " );
//			for (T t: fifo)
//				System.out.print(t + " ");
//			System.out.println();
//		}
		
		// Final merge
		ExternalFIFO<T>[] out = new ExternalFIFO[threads];
		out[0] = new ExternalFIFO<T>(); // only need one
		merge(results, out, comparator, Integer.MAX_VALUE);
		return out[0];
		
	}
	
	
	private static <T extends Serializable> void write(List<T> set, ExternalFIFO<T>[] out, Comparator<T> comparator) throws IOException {
		Collections.sort(set, comparator);
		Iterator<T> it = set.iterator();
outer:	while (it.hasNext()) {
			for (ExternalFIFO<T> fifo: out) {
				if (!it.hasNext())
					break outer;
				fifo.add(it.next());
			}
		}
		set.clear();
	}
	
	public static <T extends Serializable & Comparable<T>> Iterable<T> sort(Iterable<T> input) throws IOException {
		return sort(input, new Comparator<T>() {
				public int compare(T o1, T o2) {
					return o1.compareTo(o2);
				}
			}
		);
	}
	
	@SuppressWarnings("unchecked")
	private static  <T extends Serializable> ExternalFIFO<T>[] newOutputArray() {
		ExternalFIFO[] ret = new ExternalFIFO[tapes];
		for (int i = 0; i < tapes; i++)
			ret[i] = new ExternalFIFO();
		return ret;
	}

	/**
	 * Merge one set of FIFOs into another, assuming that input elements are already ordered in 
	 * <code>blockSize</code> blocks. <code>outputs</code> will be filled, round-robin style, with
	 * blocks of size <code>blockSize * 2</code>. The total number of blocks is returned. So if
	 * the method returns <code>1</code>, the merge has produced a final sorted list, which will
	 * be the entire contents of <code>outputs[0]</code>.
	 * @param <T>
	 * @param inputs
	 * @param outputs
	 * @param comparator
	 * @param blockSize
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	static <T extends Serializable> int merge(ExternalFIFO<T>[] inputs, ExternalFIFO<T>[] outputs, Comparator<T> comparator, int blockSize) throws IOException {

		assert outputs.length == inputs.length;

		int tapes = outputs.length;
		
		Iterator<T>[] iterators = new Iterator[inputs.length];
		for (int i = 0; i < iterators.length; i++)
			iterators[i] = inputs[i].iterator();
		
		// New iterators, counts, and elements
		Object[] t = new Object[tapes];
		int[] c = new int[tapes];

		// Merge <blockSize> pairs. Each time around we write one element
		long tt = System.currentTimeMillis();
		int blocks = 0;
		
		for (;;) {
			
			// At least one iterator should be non-empty
			boolean done = true;
			for (Iterator<T> i: iterators) {
				if (i.hasNext()) {
					done = false;
					break;
				}
			}
			if (done)
				break;
			
			// All should be null
			for (Object o: t)
				assert o == null;

			// All counts should be 0
			for (int i = 0; i < tapes; i++)
				c[i] = 0;
			
			ExternalFIFO<T> currentFifo = outputs[blocks % tapes];
			
			for (long element = 0; element < (blockSize * (long) tapes); element++) {
				
				// Grab the next element if needed
				for (int i = 0; i < tapes; i++)
					if (t[i] == null && iterators[i].hasNext() && c[i]++ < blockSize) 
						t[i] = iterators[i].next();
				
				// Find smallest
				// TODO: cache previous smallest
				int which = -1;
				for (int i = 0; i < tapes; i++) {
					if (t[i] != null) {
						if (which == -1) {
							which = i;
						} else {
							which = comparator.compare((T) t[which], (T) t[i]) < 0 ? which : i;
						}
					}
				}
				if (which == -1)
					break;
									
				// Do it
				currentFifo.add((T) t[which]);
				t[which] = null;
				
			}
			++blocks;
		}
		
		tt = System.currentTimeMillis() - tt;
		LOGGER.fine("Merged " + blocks + " pair(s) of size " + blockSize + " in " + tt + "ms.");
		return blocks;
		
	}
	
}
