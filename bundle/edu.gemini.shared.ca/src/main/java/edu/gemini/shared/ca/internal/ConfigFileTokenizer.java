package edu.gemini.shared.ca.internal;

import java.nio.CharBuffer;
import java.util.NoSuchElementException;


/**
 * A CountingTokenizer that understands some peculiar aspects of the CA config file syntax,
 * including the notion of things having to be on a single line, and the concept of
 * "everything from here to the end of the line is a comment".
 * @author rnorris
 */
public class ConfigFileTokenizer extends CountingTokenizer {

	private final CharBuffer buf;
	
	/**
	 * Creates a new ChannelAccessDBTokenizer. <i>Note that this tokenizer does not
	 * parse comments. The buffer should be cleansed of comments before constructing
	 * the tokenizer. The tokenizer doesn't do it for you because any ethical tokenizer
	 * won't modify its input.</i>
	 * @param buf a CharBuffer containing a CA file, cleansed of comments
	 */
	public ConfigFileTokenizer(CharBuffer buf) {
		super(buf);
		this.buf = buf.asReadOnlyBuffer();
	}

	/**
	 * Returns the remaining text on the line as a whitespace-trimmed but
	 * untokenized String. Good for end-of-line comments in CA configs.
	 * @return The remainder of the current line, or null if we're at EOL
	 */
	public String restOfLine() {
		if (eol())
			return null;
		int nextLineStart = getNextLineStart();
		char[] last = new char[nextLineStart - getNextTokenPos()];
		buf.position(getNextTokenPos());
		buf.get(last);
		do {
			next();
		} while (!eol());
		return new String(last).trim();
	}

	/**
	 * Returns the next token on the current line.
	 * @return next token on the current line
	 * @throws NoSuchElementException if there are no more tokens on the line.
	 */
	public String nextOnLine() {
		if (eol())
			throw new NoSuchElementException("Unexpected EOL.");
		return nextToken();
	}
	
	/**
	 * Looks for and eats an opening brace. Use this with endBlock() to
	 * parse brace-enclosed sub-expressions:
	 * <pre>
	 * 		cat.beginBlock();
	 * 		while (!cat.endBlock()) {
	 * 			// process the next token, line, etc., from
	 * 			// the nested block.
	 * 		}
	 * </pre>
	 * @throws NoSuchElementException if the next token is not an opening brace.
	 */
	public void beginBlock() {
		if (!nextToken().equals("{"))
			throw new NoSuchElementException("Expected opening brace.");
	}
	
	/**
	 * Looks for and eats a closing brace, returning true if successful
	 * or false if the next token is not a closing brace. Use this with
	 * beginBlock() to parse brace-enclosed sub-expressions.
	 * @return true if the block was ended, false otherwise
	 */
	public boolean endBlock() {
		if (peek().equals("}")) {
			next();
			return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the current token is the last one on the line.
	 * @return true if there are no more tokens on this line.
	 */
	public boolean eol() {
		return getNextTokenPos() >= getNextLineStart();
	}
	
}
