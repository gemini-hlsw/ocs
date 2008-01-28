#ifndef GIAPI_H_
#define GIAPI_H_


namespace giapi {
	namespace status {
		/**
		 * GIAPI Status. Used for GIAPI calls to report status of the 
		 * different library calls to the invoker. 
		 */
		typedef enum {
			GIAPI_NOK = -1, //Errors that don't have a status
			GIAPI_OK = 0, //Success
			GIAPI_WARNING, //Some warning condition
			GIAPI_INVALIDOBJ, //An object is invalid
			GIAPI_INVALIDARG, //An argument is bad
			GIAPI_CONVERT_ERROR, //An argument failed to convert
			GIAPI_NOMEMORY, // No memory available
			GIAPI_POST_ERROR, //Failed to post
			GIAPI_NOTFOUND, //Something was not found
			GIAPI_FOUND //Status: Something was found. 
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
			VOID, //Uninitialized. Can't operate if the type is set to VOID
			INT,
			CHAR,
			STRING,
			BOOL,
			DOUBLE
		} Type;
		
	}
}

#endif /*GIAPI_H_*/

