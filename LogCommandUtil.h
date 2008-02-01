#ifndef LOGCOMMANDUTIL_H_
#define LOGCOMMANDUTIL_H_

#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

namespace giapi {
/**
 * A Command util that class that logs the requests.
 */
class LogCommandUtil {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	int subscribeSequenceCommand(command::SequenceCommand id,
			command::ActivitySet activities, pSequenceCommandHandler handler);

	int subscribeApply(const char* prefix, command::ActivitySet activities,
			pSequenceCommandHandler handler);

	int postCompletionInfo(command::ActionId id, pHandlerResponse response);

	static LogCommandUtil& Instance();
	virtual ~LogCommandUtil();
	
private:
	static LogCommandUtil * INSTANCE;
	LogCommandUtil();
};

}

#endif /*LOGCOMMANDUTIL_H_*/
