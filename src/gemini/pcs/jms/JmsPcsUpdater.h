
#ifndef JMSPCSUPDATER_H_
#define JMSPCSUPDATER_H_

#include <giapi/giapiexcept.h>
#include <gemini/pcs/PcsUpdater.h>
#include <util/jms/JmsProducer.h>

namespace giapi {
namespace gemini {
namespace pcs {
namespace jms {


/**
 * An implementation of the PcsUpdater interface using JMS
 * as the connection mechanism.
 */
class JmsPcsUpdater: public PcsUpdater,  util::jms::JmsProducer {
public:
	virtual ~JmsPcsUpdater();

	int postPcsUpdate(double zernikes[], int size) throw (GiapiException);

	/**
	 * Static factory method to instantiate a new JmsPcsUpdater object
	 * and obtain a smart pointer to access it.
	 */
	static pPcsUpdater create() throw (CommunicationException);
private:
	/**
	 * Private Constructor.
	 */
	JmsPcsUpdater() throw (CommunicationException);
};

}
}
}
}

#endif /* JMSPCSUPDATER_H_ */
