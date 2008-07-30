#ifndef JMSCOMMANDUTIL_H_
#define JMSCOMMANDUTIL_H_


#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

#include <gmp/SequenceCommandConsumer.h>

#include <util/giapiMaps.h>


namespace giapi {


/**
 * The ActivityHolder class is a container that is used to keep track
 * of the different <code>gmp::SequenceCommandConsumer</code> objects
 * associated to each <code>command::Activity</code> element.
 * 
 * The ActivityHolder is in charge of having a <i>unique</i> 
 * <code>gmp::SequenceCommandConsumer</code> for each
 * <code>command::Activity</code>. If a new consumer is registered
 * for an Activity, the old one is discarded (and deleted
 * if nobody else is using it) 
 * 
 * The CommandUtil class associates an <code>ActivityHolder</code> to 
 * each <code>command::SequenceCommand</code>. 
 */
class ActivityHolder {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	ActivityHolder();

	void registerConsumer(command::ActivitySet set,
			gmp::pSequenceCommandConsumer consumer);

	void registerConsumer(command::Activity activity,
			gmp::pSequenceCommandConsumer consumer);

	virtual ~ActivityHolder();

private:
	/**
	 * Type definition for the hash_table that will map command Ids to 
	 * the JMS Topic associated to them.
	 */
	typedef hash_map<command::Activity, gmp::pSequenceCommandConsumer>
			ActivityConsumerMap;

	ActivityConsumerMap _activityConsumerMap;
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

	typedef hash_map<command::SequenceCommand, ActivityHolder *>
			CommandHolderMap;

	CommandHolderMap _commandHolderMap;

};

}

#endif /*JMSCOMMANDUTIL_H_*/
