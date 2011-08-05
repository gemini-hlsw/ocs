/*
 * StatusSerializerVisitor.cpp
 *
 *  Created on: Jan 21, 2009
 *      Author: anunez
 */

#include <status/senders/jms-writer/StatusSerializerVisitor.h>

namespace giapi {

StatusSerializerVisitor::StatusSerializerVisitor(BytesMessage *msg) {
	_msg = msg;
}

StatusSerializerVisitor::~StatusSerializerVisitor() {

}

void StatusSerializerVisitor::visitStatusItem(StatusItem *item)
		throw (CMSException) {
	writeHeader(BASIC_OFFSET, item);
}

void StatusSerializerVisitor::visitAlarmItem(AlarmStatusItem * alarm)
		throw (CMSException) {
	writeHeader(ALARM_OFFSET, alarm);

	alarm::Cause cause = alarm->getCause();
	alarm::Severity severity = alarm->getSeverity();

	//add severity to the message
	switch (severity) {
	case alarm::ALARM_OK:
		_msg->writeByte(0);
		break;
	case alarm::ALARM_WARNING:
		_msg->writeByte(1);
		break;
	case alarm::ALARM_FAILURE:
		_msg->writeByte(2);
		break;
	}
	//add cause
	switch (cause) {
	case alarm::ALARM_CAUSE_OK:
		_msg->writeByte(0);
		break;
	case alarm::ALARM_CAUSE_HIHI:
		_msg->writeByte(1);
		break;
	case alarm::ALARM_CAUSE_HI:
		_msg->writeByte(2);
		break;
	case alarm::ALARM_CAUSE_LOLO:
		_msg->writeByte(3);
		break;
	case alarm::ALARM_CAUSE_LO:
		_msg->writeByte(4);
		break;
	case alarm::ALARM_CAUSE_OTHER:
		_msg->writeByte(5);
		break;
	}

	//check if there is any message for this alarm
	if (!(alarm->getMessage().empty())) {
		_msg->writeBoolean(true); //there is a message
		_msg->writeUTF(alarm->getMessage());
	} else {
		_msg->writeBoolean(false); //no message
	}

}

void StatusSerializerVisitor::visitHealthItem(HealthStatusItem * item)
		throw (CMSException) {
	writeHeader(HEALTH_OFFSET, item);
}

void StatusSerializerVisitor::writeHeader(int offset, StatusItem *item)
		throw (CMSException) {

	const type::Type type = item->getStatusType();
	std::string value;

	switch (type) {
	case type::INT:
		_msg->writeByte(offset);
		//the name now...
		_msg->writeUTF(item->getName());
		//and finally the value
		_msg->writeInt(item->getValueAsInt());
        //and then the timestamp
        _msg->writeLong(item->getTimestamp());
		break;
	case type::DOUBLE:
		_msg->writeByte(offset + 1);
		//the name now...
		_msg->writeUTF(item->getName());
		//and finally the value
		_msg->writeDouble(item->getValueAsDouble());
        //and then the timestamp
        _msg->writeLong(item->getTimestamp());
		break;
	case type::FLOAT:
		_msg->writeByte(offset + 2);
		//the name now...
		_msg->writeUTF(item->getName());
		//and finally the value
		_msg->writeFloat(item->getValueAsFloat());
        //and then the timestamp
        _msg->writeLong(item->getTimestamp());
		break;

		break;
	case type::STRING:
		_msg->writeByte(offset + 3);
		//the name now...
		_msg->writeUTF(item->getName());
		//and finally the value
	    value = item->getValueAsString();
	    //writeUTF method doesn't work with an empty string.
	    //in that case, send one whitespace instead
		if (value == "") {
			value = " ";
		}
		_msg->writeUTF(value);
        //and then the timestamp
        _msg->writeLong(item->getTimestamp());
		break;
	case type::BOOLEAN:
		//TODO: Add the boolean handling when
		//supported by the GIAPI
		break;

	case type::BYTE:
		//TODO: Add the Byte handler when/if supported by the GIAPI
		break;

	case type::SHORT:
		//TODO: Add the Byte handler when/if supported by the GIAPI
		break;


	}

}

}
