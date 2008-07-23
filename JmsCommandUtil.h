#ifndef JMSCOMMANDUTIL_H_
#define JMSCOMMANDUTIL_H_


#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

#include <gmp/SequenceCommandConsumer.h>

#include <util/giapiMaps.h>


namespace giapi {

class ActivityHolder {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	ActivityHolder();

	gmp::SequenceCommandConsumer * getConsumer(command::Activity activity);

	void registerConsumer(command::ActivitySet set,
			gmp::SequenceCommandConsumer * consumer);

	void registerConsumer(command::Activity activity,
			gmp::SequenceCommandConsumer * consumer);

	void unregisterConsumer(command::Activity activity);

	virtual ~ActivityHolder();

private:
	/**
	 * Type definition for the hash_table that will map command Ids to 
	 * the JMS Topic associated to them.
	 */
	typedef hash_map<command::Activity, gmp::SequenceCommandConsumer *>
			ActivityConsumerMap;

	ActivityConsumerMap _activityConsumerMap;

	/**
	 * Determine whether the consumer associated to the  activity is the same
	 * as the one specified in the <code>consumer</code> argument
	 */
	bool sameConsumer(command::Activity activity,
			gmp::SequenceCommandConsumer * consumer);

};

class JmsCommandUtil {

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

	static JmsCommandUtil& Instance();

	virtual ~JmsCommandUtil();

private:
	/**
	 * Internal instance of this utility class
	 */
	static std::auto_ptr<JmsCommandUtil> INSTANCE;
	JmsCommandUtil();

	void storeConsumerPointer(gmp::SequenceCommandConsumer * consumer,
			command::SequenceCommand sequenceCommand,
			command::ActivitySet activities);

	typedef hash_map<command::SequenceCommand, ActivityHolder *>
			CommandHolderMap;

	CommandHolderMap _commandHolderMap;

};

}

#endif /*JMSCOMMANDUTIL_H_*/
