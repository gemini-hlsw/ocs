package edu.gemini.shared.ca.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tokenizer that keeps track of current line and character positions. The line and
 * character positions are useful for reporting parse errors to the user; wherever parsing
 * fails, you can simply report that the failure happened near line X, char Y.
 * <p>
 * A token is any sequence of non-whitespace characters.
 * <p>
 * Note that the constructor takes a CharSequence rather than a stream. So basically you 
 * can pass a String or a CharBuffer (which you can obtain easily by mapping a file into 
 * memory using the java.nio API):
 * <pre>
 * 	Charset encoding = Charset.forName("US-ASCII"); // or whatever
 * 	FileChannel fc = new FileInputStream(inputFile).getChannel();
 * 	MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
 * 	CountingTokenizer ct = new CountingTokenizer(encoding.decode(bb));
 * </pre>
 * There are a few protected methods that provide information useful for subclasses that
 * may wish to look ahead or examine absolute buffer offsets.
 */
public class CountingTokenizer implements Iterator {

	private static final Pattern PAT_WORD = Pattern.compile("\\S+", Pattern.MULTILINE);
	private static final Pattern PAT_LINE = Pattern.compile("^.*", Pattern.MULTILINE);

	private final Matcher matcher;
	private final Matcher lineMatcher;
	
	private String next;
	private int line;
	private int linePos;
	private int pos;
	private int thisLineStart;
	private boolean moreLines = true;
	
	/**
	 * Constructs a new CountingTokenizer with the given CharSequence.
	 * @param seq a CharSequence
	 */
	public CountingTokenizer(CharSequence seq) {
		matcher = PAT_WORD.matcher(seq);
		lineMatcher = PAT_LINE.matcher(seq);
		lineMatcher.find();
		advance();
	}

	/**
	 * Finds the next token. We always maintain a lookahead of one so we can answer
	 * hasNext().
	 */
	private void advance() {
		if (matcher.find()) {
			next = matcher.group();
		} else {
			next = null;
		}		
	}
	
	public boolean hasNext() {
		return next != null;
	}

	public Object next() {
		if (hasNext()) {
			String ret = next;
			pos = matcher.start();
			while (moreLines && lineMatcher.start() <= pos) {
				thisLineStart = lineMatcher.start();
				moreLines = lineMatcher.find();
				line++;
			}
			linePos = pos - thisLineStart;
			advance();
			return ret;
		}
		throw new NoSuchElementException("Unexpected EOF");
	}

	/**
	 * Convience method that simply returns <code>(String) next()</code>.
	 * @return the next token
	 */
	public String nextToken() {
		return (String) next();
	}

	/**
	 * This operation is unsupported.
	 * @throws UnsupportedOperationException always
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the current line in the input sequence. This value is 1-based.
	 * @return the current line number, or 0 if next() has not been called
	 */
	public int getLine() {
		return line;
	}
	
	/**
	 * Returns the current char position in the current line. This value is 1-based.
	 * @return the current character position in the current line
	 */
	public int getPosInLine() {
		return linePos + 1;
	}
	
	/**
	 * Returns the character offset of the next token to be returned, 0-based from
	 * the start of the input sequence.
	 * @return the sequence offset for the next token to be returned
	 * @throws IllegalStateException if there are no more tokens
	 */
	protected int getNextTokenPos() {
		return matcher.start();
	}
	
	/**
	 * Returns the character offset for the beginning of the next line.
	 * @return the sequence offset for the next line.
	 * @throws IllegalStateException if there are no more tokens
	 */
	protected int getNextLineStart() {
		return lineMatcher.start();
	}

	/**
	 * Returns true if there are more lines.
	 * @return true if there are more lines.
	 */
	protected boolean hasMoreLines() {
		return moreLines;
	}

	/**
	 * Returns the next token but does not advance any of the position
	 * counters.
	 * @return the next token
	 */
	protected String peek() {
		return next;
	}
	
}
