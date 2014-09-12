package edu.gemini.pit.ui.util;

public enum DegreePreference {

	DEGREES,
	HMSDMS	
	
	;
	
	public static EnumBox<DegreePreference> BOX = new EnumBox<DegreePreference>(HMSDMS);
	
}
