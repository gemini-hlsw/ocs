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
			ALARM_OK,
			/**
			 * A WARNING is an alarm that subsystems see as important and should
			 * be brought to the attention of the users. The subsystems should
			 * be able to continue despite warning events. 
			 */
			ALARM_WARNING,
			/**
			 * A FAILURE indicates that a component is in a state that will not
			 * allow to continue operations. 
			 */
			ALARM_FAILURE
		} Severity;
		
		/**
		 * Cause of the alarm
		 */
		typedef enum {
			ALARM_CAUSE_OK,
			ALARM_CAUSE_HIHI,
			ALARM_CAUSE_HI,
			ALARM_CAUSE_LOLO,
			ALARM_CAUSE_LO,
			ALARM_CAUSE_OTHER
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
	
	namespace health {
		/**
		 * Health values
		 */
		typedef enum  {
			/**
			 * Good health. The system/suybsystem is normal 
			 */
			GOOD,
			/**
			 * Warning health. The system/subsystem is operating, but
			 * not normally. 
			 */
			WARNING,
			/**
			 * Bad health. The system/subsystem is not operating.
			 */
			BAD
		} Health;
	}
	
	namespace command {
		typedef int ActionId;
		
		typedef enum {
			TEST,
			REBOOT,
			INIT,
			DATUM,
			PARK,
			VERIFY,
			END_VERIFY,
			GUIDE,
			END_GUIDE,
			APPLY, 
			OBSERVE,
			END_OBSERVE,
			PAUSE,
			CONTINUE,
			STOP,
			ABORT
		} SequenceCommand;
		
		typedef enum {
			PRESET,
			START,
			PRESET_START,
			CANCEL
		} Activity;
	}
}

#endif /*GIAPI_H_*/

