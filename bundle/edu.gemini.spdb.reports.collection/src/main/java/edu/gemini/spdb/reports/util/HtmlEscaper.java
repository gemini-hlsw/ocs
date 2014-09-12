package edu.gemini.spdb.reports.util;

import java.util.Date;

public final class HtmlEscaper {

	public String escape(String s) {
		if (s == null)
			return s;
		StringBuilder sb = new StringBuilder();
		for (char c: s.toCharArray()) {
			switch (c) {
			case '<': sb.append("&lt;"); break;
			case '>': sb.append("&gt;"); break;
			case '"': sb.append("&quot;"); break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public String escapeWithBreaks(String s) {
		return escape(s).replace("\n", "<br>");
	}
	
	public String longToDate(long d) {
		return new Date(d).toString();
	}
	
}
