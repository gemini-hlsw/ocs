#ifndef STATUSFACTORYIMPL_H_
#define STATUSFACTORYIMPL_H_
#include "giapi/StatusFactory.h"

namespace giapi {

class StatusFactoryImpl : public StatusFactory {
private:
	StatusSender* senders[StatusFactory::Elements];
	static const StatusSenderType DEFAULT_SENDER = LogSender;
public:
	StatusFactoryImpl();
	virtual ~StatusFactoryImpl();
	virtual StatusSender& getStatusSender();
	virtual StatusSender& getStatusSender(StatusSenderType type);

};
}

#endif /*STATUSFACTORYIMPL_H_*/
