/*
 * StatusSerializerVisitor.h
 *
 *  Created on: Jan 21, 2009
 *      Author: anunez
 */

#ifndef STATUSSERIALIZERVISITOR_H_
#define STATUSSERIALIZERVISITOR_H_

#include <log4cxx/logger.h>

#include <status/StatusVisitor.h>
#include <status/StatusItem.h>
#include <status/AlarmStatusItem.h>
#include <status/HealthStatusItem.h>

#include <cms/BytesMessage.h>
#include <cms/CMSException.h>

namespace giapi {

using namespace cms;

/**
 * This visitor is used to code the status items objects into
 * JMS messages. The visitor takes as an argument the JMS message
 * (in the form of a BytesMessage) that will be filled in with
 * the content of the appropriate status item.
 */
class StatusSerializerVisitor :public StatusVisitor {

private:
	/**
	 * The message that will be filled in by this visitor
	 */
	BytesMessage * _msg;

	/**
	 * Offsets used to serialize the messages and distinguish
	 * among the different types.
	 */
	enum Offsets {
		BASIC_OFFSET = 0,
		ALARM_OFFSET = 10,
		HEALTH_OFFSET = 20
	};

	/**
	 * Auxiliary method to write  common information
	 * for all the status items, like the name and their
	 * value. The offset is used when coding the
	 * message to distinguish the different types of
	 * status when reconstructing.
	 */
	void writeHeader(int offset, StatusItem *item) throw (CMSException);

public:
	StatusSerializerVisitor(BytesMessage *msg);
	virtual ~StatusSerializerVisitor();

	/**
	 * Serialize a Status Item into the JMS message
	 */
	void visitStatusItem(StatusItem * item) throw (CMSException);

	/**
	 * Serialize the Alarm Status Item into the JMS message
	 */
	void visitAlarmItem(AlarmStatusItem * item) throw (CMSException);

	/**
	 * Serialize the Health into the JMS Message
	 */
	void visitHealthItem(HealthStatusItem * item) throw (CMSException);
};

}

#endif /* STATUSSERIALIZERVISITOR_H_ */
