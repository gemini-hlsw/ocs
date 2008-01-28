#include <giapi/StatusFactory.h>
#include "StatusFactoryImpl.h"

namespace giapi {
StatusFactory* StatusFactory::INSTANCE = 0;

StatusFactory::StatusFactory() {
}

StatusFactory::~StatusFactory() {
}

StatusFactory& StatusFactory::Instance() {
	if (INSTANCE == 0) {
		INSTANCE = new StatusFactoryImpl();
	}
	return *INSTANCE;
}

}
