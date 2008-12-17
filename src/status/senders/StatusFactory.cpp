#include "StatusFactory.h"
#include "StatusFactoryImpl.h"

namespace giapi {
std::auto_ptr<StatusSenderFactory> StatusSenderFactory::INSTANCE(new StatusSenderFactoryImpl());

StatusSenderFactory::StatusSenderFactory() {
}

StatusSenderFactory::~StatusSenderFactory() {
}

StatusSenderFactory& StatusSenderFactory::Instance() {
	return *INSTANCE;
}

}
