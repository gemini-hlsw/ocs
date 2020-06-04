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
			OK = 0 //Success
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
			BOOLEAN,
			INT,
			FLOAT,
			DOUBLE,
			STRING,
			BYTE,
			SHORT
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
			STOP_CYCLE,
			ABORT,
            ENGINEERING
		};

		enum Activity {
			PRESET,
			START,
			PRESET_START,
			CANCEL
		};

		enum ActivitySet {
			SET_PRESET,
			SET_START,
			SET_PRESET_START,
			SET_CANCEL,
			SET_PRESET_CANCEL,
			SET_START_CANCEL,
			SET_PRESET_START_CANCEL
		};

	}
	namespace log {
		enum Level {
				Level_begin,

				INFO = Level_begin,
				WARNING,
				SEVERE,

				Level_end
		};

	}

	namespace data {
		enum ObservationEvent {
			/**
			 * Event sent as instrument starts preparation for starting
			 * acquisition of a dataset.
			 */
			OBS_PREP,
			/**
			 * Event sent just before data acquisition starts.
			 */
			OBS_START_ACQ,
			/**
			 * Event sent when the requested acquisition has completed.
			 */
			OBS_END_ACQ,
			/**
			 * Event indicates that data is being transferred from the
			 * detector or other activities needed to write data.
			 */
			OBS_START_READOUT,
			/**
			 * Event indicates readout or write preparations have completed
			 */
			OBS_END_READOUT,
			/**
			 * Event indicates that the instrument has started writing
			 * the dataset to GSDN
			 */
			OBS_START_DSET_WRITE,
			/**
			 * Event indicates that the instrument has completed writing
			 * the dataset to GSDN
			 */
			OBS_END_DSET_WRITE
		};
	}
	/**
	 * The TCS Context structure.
	 */
	struct TcsContext {
		double time;      //Gemini raw time
		double x,y,z;     //Cartesian elements of mount pre-flexure az/el
		//Telescope Parameters structure
		struct Tel {
			double fl;    //Telescope focal length (mm)
			double rma;   //Rotator mechanical angle (rads)
			double an;    //Azimuth axis tilt NS (rads)
			double aw;    //Azimuth axis tilt EW (rads)
			double pnpae; //Az/El nonperpendicularity (rads)
			double ca;    //Net left-right(horizontal) collimation (rads)
			double ce;    //Net up-down(vertical) collimation (rads)
			double pox;   //Pointing origin x-component (mm)
			double poy;   //Pointing origin y-component (mm)
		} tel;
		double aoprms[15]; //Target independent apparent to observed parameters
		double m2xy[3][2]; //M2 tip/tilt (3 chop states)
		//Point Origin structure
		struct PO {
			double mx; //Mount point origin in X
			double my; //Mount point origin in Y
			double ax; //Source chop A pointing origin in X
			double ay; //Source chop A pointing origin in Y
			double bx; //Source chop B pointing origin in X
			double by; //Source chop B pointing origin in Y
			double cx; //Source chop C pointing origin in X
			double cy; //Source chop C pointing origin in Y
		} po;
		double ao2t[6]; //Optical distortion coefficients (Not used to date)
	};
	/**
	 * Definition of a long with 64 bits. Since 64-bit signed integers are
	 * not part of the ANSI C++ standard, this definition is compiler
	 * specific.
	 * This works in GCC 4.X under Linux.
	 */
	typedef long long long64;

	/**
	 * Definition of an error handler. GIAPI error handlers can be registered
	 * in the GIAPI using the GiapiUtil class.
	 */
	typedef void (*giapi_error_handler)(void);
}



#endif /*GIAPI_H_*/

