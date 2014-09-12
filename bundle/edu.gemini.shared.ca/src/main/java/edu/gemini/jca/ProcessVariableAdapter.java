package edu.gemini.jca;

public interface ProcessVariableAdapter<JT, ET> {

	ET toEpics(JT javaValue);
	
	JT fromEpics(ET epicsValue);
	
}
