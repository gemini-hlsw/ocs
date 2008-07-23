#ifndef GMPKEYS_H_
#define GMPKEYS_H_

#include<string>

namespace gmp {

class GMPKeys {
public:
	//General purpose keys
	const static std::string GMP_PREFIX;
	const static std::string GMP_SEPARATOR;
	const static std::string GMP_SEQUENCE_COMMAND_PREFIX;
	
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
	const static std::string GMP_SEQUENCE_COMMAND_ABORT;
		
														
	
	
	virtual ~GMPKeys();
	
private:
	GMPKeys();
};

}

#endif /*GMPKEYS_H_*/
