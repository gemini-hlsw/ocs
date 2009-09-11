#include "JmsPcsUpdater.h"
#include <gmp/GMPKeys.h>

using namespace gmp;

namespace giapi {
namespace gemini {
namespace pcs {
namespace jms {

JmsPcsUpdater::JmsPcsUpdater() throw (CommunicationException) :
	JmsProducer(GMPKeys::GMP_PCS_UPDATE_DESTINATION) {

}

JmsPcsUpdater::~JmsPcsUpdater() {
}

pPcsUpdater JmsPcsUpdater::create() throw (CommunicationException) {
	pPcsUpdater updater(new JmsPcsUpdater());
	return updater;
}

int JmsPcsUpdater::postPcsUpdate(double zernikes[], int size)
		throw (GiapiException) {

	if (size <= 0)
		return status::ERROR;

	BytesMessage * msg;
	try {
		msg = _session->createBytesMessage();
		//first, the size
		msg->writeInt(size);
		//now the doubles
		for (int i = 0; i < size; i++) {
			msg->writeDouble(zernikes[i]);
		}
		_producer->send(msg);

	} catch (CMSException &e) {
		throw CommunicationException("Problem posting updates to the PCS: " + e.getMessage());
	}
	return status::OK;
}
}
}
}
}
