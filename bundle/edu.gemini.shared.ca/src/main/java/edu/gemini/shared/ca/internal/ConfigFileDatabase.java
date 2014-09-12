package edu.gemini.shared.ca.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Channel access database implementation that reads the same file format as the
 * C++ class <code>CaDB</code> located in ocswish/libChannels.
 * <p>
 * The parser tries to give good error messages.
 */
public class ConfigFileDatabase extends AbstractDatabase {

	private static final Pattern PAT_LINE_COMMENT = Pattern.compile("(//|#).*", Pattern.MULTILINE);
	private static final Pattern PAT_BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern PAT_INCLUDE_SYNTAX = Pattern.compile("^#include<(.*?)>$");
	
	public ConfigFileDatabase(File file) throws IOException {
		this(file, Charset.forName("US-ASCII"));
	}
	
	public ConfigFileDatabase(File file, Charset encoding) throws IOException {
		read(file, encoding);
	}

	private void read(File file, Charset encoding) throws IOException {
		
		// Read the file into a CharBuffer
		FileChannel fc = new FileInputStream(file).getChannel();
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		CharBuffer cb = encoding.decode(bb);

		// Erase comments in-place to preserve line number and offsets.
		eraseLineComments(cb); 
		eraseBlockComments(cb);
		
		// Now we can tokenize
		ConfigFileTokenizer tok = new ConfigFileTokenizer(cb);
		try {
			while (tok.hasNext()) {
				String token = tok.nextToken();
				
				if (token.startsWith("#include")) {
				
					// #include is a special case because space afterward is
					// optional. We will just take the next token if we find a
					// bare #include and then handle both cases the same.
					if (token.length() == 8)
						token += tok.nextToken();
					readInclude(file, token, encoding);
				
				} else if (token.equals("apply")) {
					readApply(tok);
				} else if (token.equals("status")) {
					readStatus(tok);
				} else if (token.equals("command")) {
					readCommand(tok);
				} else if (token.equals("channels")) {
					readChannels(tok);
				} else {
					throw new IOException("Expected #include, apply, status, command, or channels. Found: " + token);
				}
				
			}
		} catch (Exception e) {
			IOException ioe = new IOException("Parse error in " + file.getAbsolutePath() + " near line " + tok.getLine() + " char " + tok.getPosInLine());
			ioe.initCause(e);
			throw ioe;
		}
	}

	private void readChannels(final ConfigFileTokenizer tok) {
		tok.beginBlock();
		while (!tok.endBlock()) {
			String ocsName = tok.nextToken();
			String epicsName = tok.nextOnLine();
			int skipCount = Integer.parseInt(tok.nextOnLine());
			String comment = tok.restOfLine();
			Channel ch = createChannel(ocsName, epicsName, comment, skipCount);
			add(ch);
		}
	}

	private void readCommand(ConfigFileTokenizer tok) {
		String ocsCommandName = tok.nextOnLine();
		String applyName = tok.nextOnLine();
		String commandComment = tok.restOfLine();
		List<Entry> entries = new ArrayList<Entry>();
		tok.beginBlock();
		while (!tok.endBlock()) {
			String ocsName = tok.nextToken();
			String epicsName = tok.nextOnLine();
			String comment = tok.restOfLine();
			entries.add(createEntry(ocsName, epicsName, comment));
		}
		Command c = createCommand(ocsCommandName, applyName, commandComment, entries);
		add(c);
	}

	private void readStatus(ConfigFileTokenizer tok) {
		String ocsStatusName = tok.nextOnLine();
		String statusComment = tok.restOfLine();
		List<Entry> entries = new ArrayList<Entry>();
		tok.beginBlock();
		while (!tok.endBlock()) {
			String ocsName = tok.nextToken();
			String epicsName = tok.nextOnLine();
			String skip = tok.nextOnLine(); // TODO: do something with this
			String comment = tok.restOfLine();
			entries.add(createEntry(ocsName, epicsName, comment));
		}
		Status status = createStatus(ocsStatusName, statusComment, entries);
		add(status);
	}

	private void readApply(ConfigFileTokenizer tok) {
		String ocsName = tok.nextOnLine();
		String epicsName = tok.nextOnLine();
		String applyCarEpicsName = tok.nextOnLine();
		String comment = tok.restOfLine();
		Apply apply = createApply(ocsName, epicsName, applyCarEpicsName, comment);
		add(apply);
	}

	private void readInclude(File file, String token, Charset encoding) throws IOException {
		Matcher m = PAT_INCLUDE_SYNTAX.matcher(token);
		if (m.matches()) {
			String fname = m.group(1) + ".cfg";
			File include = (fname.charAt(0) == File.separatorChar) 
				? new File(fname) 
				: new File(file.getParentFile(), fname);
			read(include, encoding);				
		} else {
			throw new IOException("Expected #include<file>, found " + token);
		}
	}

	private void eraseLineComments(CharBuffer cb) {
		Matcher m = PAT_LINE_COMMENT.matcher(cb);
		while (m.find()) {
			if (m.group().trim().startsWith("#include"))
				continue;			
			erase(cb, m.start(), m.end());
		}
	}

	private void eraseBlockComments(CharBuffer cb) {
		Matcher m = PAT_BLOCK_COMMENT.matcher(cb);
		while (m.find())
			erase(cb, m.start(), m.end());
	}
	
	private void erase(CharBuffer cb, int start, int end) {
		for (int i = start; i < end; i++)
			if (!Character.isWhitespace(cb.get(i)))
				cb.put(i, ' ');
	}

}



