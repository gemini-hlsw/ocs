#ifndef GMPKEYS_H_
#define GMPKEYS_H_

#include<string>

namespace gmp {


/**
 * This class defines static constants that are used to exchange messages
 * with the GMP
 */
class GMPKeys {
public:
	//General purpose keys
	const static std::string GMP_PREFIX;
	const static std::string GMP_SEPARATOR;
	const static std::string GMP_SEQUENCE_COMMAND_PREFIX;

	//Specific Message topics or queues
	const static std::string GMP_COMPLETION_INFO;

	//Message Properties
	const static std::string GMP_ACTIONID_PROP;
	const static std::string GMP_ACTIVITY_PROP;

	//Message Keywords

	//Activity keywords
	const static std::string GMP_ACTIVITY_PRESET;
	const static std::string GMP_ACTIVITY_START;
	const static std::string GMP_ACTIVITY_PRESET_START;
	const static std::string GMP_ACTIVITY_CANCEL;

	//Handler Responses
	const static std::string GMP_HANDLER_RESPONSE_ACCEPTED;
	const static std::string GMP_HANDLER_RESPONSE_STARTED;
	const static std::string GMP_HANDLER_RESPONSE_COMPLETED;
	const static std::string GMP_HANDLER_RESPONSE_ERROR;

	//Handler Response Keys
	const static std::string GMP_HANDLER_RESPONSE_KEY;
	const static std::string GMP_HANDLER_RESPONSE_ERROR_KEY;

	//Sequence Command Keys
	const static std::string GMP_SEQUENCE_COMMAND_TEST;
	const static std::string GMP_SEQUENCE_COMMAND_REBOOT;
	const static std::string GMP_SEQUENCE_COMMAND_INIT;
	const static std::string GMP_SEQUENCE_COMMAND_DATUM;
	const static std::string GMP_SEQUENCE_COMMAND_PARK;
	const static std::string GMP_SEQUENCE_COMMAND_VERIFY;
	const static std::string GMP_SEQUENCE_COMMAND_END_VERIFY;
	const static std::string GMP_SEQUENCE_COMMAND_GUIDE;
	const static std::string GMP_SEQUENCE_COMMAND_END_GUIDE;
	const static std::string GMP_SEQUENCE_COMMAND_APPLY;
	const static std::string GMP_SEQUENCE_COMMAND_OBSERVE;
	const static std::string GMP_SEQUENCE_COMMAND_END_OBSERVE;
	const static std::string GMP_SEQUENCE_COMMAND_PAUSE;
	const static std::string GMP_SEQUENCE_COMMAND_CONTINUE;
	const static std::string GMP_SEQUENCE_COMMAND_STOP;
	const static std::string GMP_SEQUENCE_COMMAND_STOP_CYCLE;
	const static std::string GMP_SEQUENCE_COMMAND_ABORT;
	const static std::string GMP_SEQUENCE_COMMAND_ENGINEERING;

	//Utility Method keys
	const static std::string GMP_UTIL_REQUEST_DESTINATION;
	//type of the request
	const static std::string GMP_UTIL_REQUEST_TYPE;
	//A property request
	const static int GMP_UTIL_REQUEST_PROPERTY;
	//Property keyword
	const static std::string GMP_UTIL_PROPERTY;

	//Logging Service Keys
	const static std::string GMP_SERVICES_LOG_DESTINATION;
	const static std::string GMP_SERVICES_LOG_LEVEL;

	//Status
	const static std::string GMP_STATUS_DESTINATION_PREFIX;

	//Gemini Service Keys - EPICS Monitoring
	const static std::string GMP_EPICS_TOPIC_PREFIX;
	const static std::string GMP_GEMINI_EPICS_REQUEST_DESTINATION;
	const static std::string GMP_GEMINI_EPICS_CHANNEL_PROPERTY;

	//Gemini Service Keys - PCS Update
	const static std::string GMP_PCS_UPDATE_DESTINATION;

	//Gemini Service Key - TCS Context
	const static std::string GMP_TCS_CONTEXT_DESTINATION;

	//Gemini Service Key - EPICS get
	const static std::string GMP_GEMINI_EPICS_GET_DESTINATION;

	//Gemini Data Util Keys
	const static std::string GMP_DATA_OBSEVENT_DESTINATION;
	const static std::string GMP_DATA_OBSEVENT_NAME;
	const static std::string GMP_DATA_OBSEVENT_FILENAME;

	const static std::string GMP_DATA_FILEEVENT_DESTINATION;
	const static std::string GMP_DATA_FILEEVENT_TYPE;
	const static std::string GMP_DATA_FILEEVENT_FILENAME;
	const static std::string GMP_DATA_FILEEVENT_DATALABEL;
	const static std::string GMP_DATA_FILEEVENT_HINT;

	virtual ~GMPKeys();

private:
	GMPKeys();
};

}


#endif /*GMPKEYS_H_*/
