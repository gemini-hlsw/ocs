#ifndef LOGCOMMANDUTIL_H_
#define LOGCOMMANDUTIL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>

#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

namespace giapi {
/**
 * A Command util class that logs the requests.
 */

class LogCommandUtil;
typedef std::tr1::shared_ptr<LogCommandUtil> pLogCommandUtil;

class LogCommandUtil {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	int subscribeSequenceCommand(command::SequenceCommand id,
			command::ActivitySet activities, pSequenceCommandHandler handler);

	int subscribeApply(const std::string &prefix, command::ActivitySet activities,
			pSequenceCommandHandler handler);

	int postCompletionInfo(command::ActionId id, pHandlerResponse response);

	static pLogCommandUtil Instance();
	virtual ~LogCommandUtil();

private:
	static pLogCommandUtil INSTANCE;
	LogCommandUtil();
};

}

#endif /*LOGCOMMANDUTIL_H_*/
