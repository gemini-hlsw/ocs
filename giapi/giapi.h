#ifndef GIAPI_H_
#define GIAPI_H_


namespace giapi {

	namespace status {
		/**
		 * GIAPI Status. Used for GIAPI calls to report status of the 
		 * different library calls to the invoker. 
		 */
		enum Status {
			ERROR = -1, //Errors that don't have a status
			OK = 0, //Success
			WARNING, //Some warning condition 
		};
	}
	
	namespace alarm {
		/**
		 * Alarm severity.
		 */
		enum Severity {
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
		};
		
		/**
		 * Cause of the alarm
		 */
		enum Cause {
			ALARM_CAUSE_OK,
			ALARM_CAUSE_HIHI,
			ALARM_CAUSE_HI,
			ALARM_CAUSE_LOLO,
			ALARM_CAUSE_LO,
			ALARM_CAUSE_OTHER
		};
	}
	
	
	namespace type {
		/**
		 * The different types supported by the elements
		 * stored in status/commands
		 */
		enum Type {
			INT,
			CHAR,
			STRING,
			BOLEAN,
			DOUBLE
		};
	}
	
	namespace health {
		/**
		 * Health values
		 */
		enum Health  {
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
		};
	}
	
	namespace command {
		typedef int ActionId;
		
		enum SequenceCommand {
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
		};
		
		enum Activity {
			PRESET,
			START,
			PRESET_START,
			CANCEL
		};
		
	}
}

#endif /*GIAPI_H_*/

