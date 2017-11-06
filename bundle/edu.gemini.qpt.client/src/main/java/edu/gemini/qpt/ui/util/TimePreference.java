package edu.gemini.qpt.ui.util;

public enum TimePreference {
	LOCAL,
	UNIVERSAL,
	SIDEREAL,
	;
	
	public static EnumBox<TimePreference> BOX = new EnumBox<>(LOCAL);
}
