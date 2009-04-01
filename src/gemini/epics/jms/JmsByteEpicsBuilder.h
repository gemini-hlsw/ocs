#ifndef JMSBYTEEPICSBUILDER_H_
#define JMSBYTEEPICSBUILDER_H_

#include "JmsEpicsBuilder.h"

namespace giapi {

/**
 * An EPICS Builder to construct EPICS status item containing Bytes
 */
class JmsByteEpicsBuilder : public JmsEpicsBuilder {
public:
	JmsByteEpicsBuilder(BytesMessage * bm);
	virtual ~JmsByteEpicsBuilder();
	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSBYTEEPICSBUILDER_H_ */
