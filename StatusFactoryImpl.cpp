#include "StatusFactoryImpl.h"
#include "LogStatusSender.h"
#include "JmsStatusSender.h"

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
		case LogSender:
			senders[type] = new LogStatusSender();
			break;
		case JMSSender:
			senders[type] = new JmsStatusSender();
			break;
		default:
			//TODO: Log invalid status type
			break;
		}
	}
	return *(senders[type]);
}

StatusSender& StatusFactoryImpl::getStatusSender() {
	return getStatusSender(DEFAULT_SENDER);
}

}
