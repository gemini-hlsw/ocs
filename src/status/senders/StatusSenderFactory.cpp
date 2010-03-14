#include "StatusSenderFactory.h"
#include "StatusSenderFactoryImpl.h"

namespace giapi {
pStatusSenderFactory StatusSenderFactory::INSTANCE(new StatusSenderFactoryImpl());

StatusSenderFactory::StatusSenderFactory() {
}

StatusSenderFactory::~StatusSenderFactory() {
}

pStatusSenderFactory StatusSenderFactory::Instance() {
	return INSTANCE;
}

}
