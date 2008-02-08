#include <giapi/StatusFactory.h>
#include "StatusFactoryImpl.h"

namespace giapi {
std::auto_ptr<StatusFactory> StatusFactory::INSTANCE(new StatusFactoryImpl());

StatusFactory::StatusFactory() {
}

StatusFactory::~StatusFactory() {
}

StatusFactory& StatusFactory::Instance() {
	return *INSTANCE;
}

}
