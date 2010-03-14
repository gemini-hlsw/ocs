#include <giapi/GiapiUtil.h>
#include <gmp/ConnectionManager.h>

using namespace gmp;

namespace giapi {

GiapiUtil::GiapiUtil() {

}

GiapiUtil::~GiapiUtil() {

}

void GiapiUtil::registerGmpErrorHandler(giapi_error_handler handler)
		throw (CommunicationException) {

	try {
		pConnectionManager manager = ConnectionManager::Instance();
		manager->registerOperation(handler);
	} catch (CMSException& e) {
		throw CommunicationException(
				"Trouble using GIAPI Utility facilities "
						+ e.getMessage());
	}

}


void GiapiUtil::registerGmpErrorHandler(pGiapiErrorHandler handler)
		throw (CommunicationException) {

	try {
		pConnectionManager manager = ConnectionManager::Instance();
		manager->registerHandler(handler);
	} catch (CMSException& e) {
		throw CommunicationException(
				"Trouble using GIAPI Utility facilities "
						+ e.getMessage());
	}

}

}
