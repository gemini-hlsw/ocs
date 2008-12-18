#include "StatusSenderFactoryImpl.h"
#include <status/senders/LogStatusSender.h>
#include <status/senders/JmsStatusSender.h>

namespace giapi {

StatusSenderFactoryImpl::StatusSenderFactoryImpl() {
	for (int i = 0; i < StatusSenderFactory::Elements; i++) {
		senders[i] = pStatusSender((StatusSender *)0);
	}
}

StatusSenderFactoryImpl::~StatusSenderFactoryImpl() {
	for (int i = 0; i < StatusSenderFactory::Elements; i++) {
		senders[i] = pStatusSender((StatusSender *)0);
	}
}

pStatusSender StatusSenderFactoryImpl::getStatusSender(StatusSenderType type) {
	if (senders[type] == 0) {
		switch (type) {
		case LOG_SENDER:
			senders[type] = pStatusSender(new LogStatusSender());
			break;
		case JMS_SENDER:
			senders[type] = pStatusSender(new JmsStatusSender());
			break;
		default:
			//return the default sender
			return getStatusSender();
		}
	}
	return (senders[type]);
}

pStatusSender StatusSenderFactoryImpl::getStatusSender() {
	return getStatusSender(DEFAULT_SENDER);
}

}
