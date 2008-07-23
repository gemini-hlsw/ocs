#ifndef JMSUTIL_H_
#define JMSUTIL_H_

#include <string>

#include <log4cxx/logger.h>

#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>

#include <util/giapiMaps.h>

using namespace giapi;
namespace gmp {
class JmsUtil {

	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	static std::string getTopic(command::SequenceCommand id);

	static command::Activity getActivity(const std::string & string);

	static std::string getHandlerResponse(pHandlerResponse response);

	virtual ~JmsUtil();
private:

	StringActionIdMap activityMap;
	ResponseStringMap handlerResponseMap;
	SequenceCommandStringMap sequenceCommandMap;
	
	JmsUtil();

	/**
	 * Get the unique instance of the Jms Util
	 * 
	 * @return The JmsUtil singleton object
	 */
	static JmsUtil & Instance();
	
	/**
	 * The singleton instance of this utility class. 
	 * Private since it's used internally only, through the
	 * static methods
	 */
	static std::auto_ptr<JmsUtil> INSTANCE;
};

}

#endif /*JMSUTIL_H_*/
