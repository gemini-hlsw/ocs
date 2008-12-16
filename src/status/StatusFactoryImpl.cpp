#include "StatusFactoryImpl.h"
#include <status/senders/LogStatusSender.h>
#include <status/senders/JmsStatusSender.h>

namespace giapi {

StatusFactoryImpl::StatusFactoryImpl() {
	for (int i = 0; i < StatusFactory::Elements; i++) {
		senders[i] = 0;
	}
}

StatusFactoryImpl::~StatusFactoryImpl() {
	for (int i = 0; i < StatusFactory::Elements; i++) {
		if (senders[i] != 0) {
			delete senders[i];
		}
	}
}

StatusSender& StatusFactoryImpl::getStatusSender(StatusSenderType type) {
	if (senders[type] == 0) {
		switch (type) {
		case LOG_SENDER:
			senders[type] = new LogStatusSender();
			break;
		case JMS_SENDER:
			senders[type] = new JmsStatusSender();
			break;
		default:
			//return the default sender
			return getStatusSender();
		}
	}
	return *(senders[type]);
}

StatusSender& StatusFactoryImpl::getStatusSender() {
	return getStatusSender(DEFAULT_SENDER);
}

}
