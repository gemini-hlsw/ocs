#ifndef GIAPI_H_
#define GIAPI_H_


namespace giapi {
	namespace status {
		/**
		 * GIAPI Status. Used for GIAPI calls to report status of the 
		 * different library calls to the invoker. 
		 */
		typedef enum {
			ERROR = -1, //Errors that don't have a status
			OK = 0, //Success
			WARNING, //Some warning condition 
		} Status;
	}
	
	namespace alarm {
		/**
		 * Alarm severity.
		 */
		typedef enum {
			/**
			 * No alarm. 
			 */
			NO_ALARM,
			/**
			 * A WARNING is an alarm that subsystems see as important and should
			 * be brought to the attention of the users. The subsystems should
			 * be able to continue despite warning events. 
			 */
			WARNING,
			/**
			 * A FAILURE indicates that a component is in a state that will not
			 * allow to continue operations. 
			 */
			FAILURE
		} Severity;
		
		/**
		 * Cause of the alarm
		 */
		typedef enum {
			NO_CAUSE,
			HIHI,
			HIGH,
			LOW,
			LOLO,
			OTHER
		} Cause;
	}
	
	
	namespace type {
		/**
		 * The different types supported by the elements
		 * stored in status/commands
		 */
		typedef enum {
			INT,
			CHAR,
			STRING,
			BOLEAN,
			DOUBLE
		} Type;
		
	}
}

#endif /*GIAPI_H_*/

