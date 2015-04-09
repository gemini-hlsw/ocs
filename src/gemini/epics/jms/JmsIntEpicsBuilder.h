#ifndef JMSINTEPICSBUILDER_H_
#define JMSINTEPICSBUILDER_H_

#include <string.h>
#include "JmsEpicsBuilder.h"

namespace giapi {
/**
 * An EPICS Builder to construct EPICS status item containing Integer values
 */

class JmsIntEpicsBuilder: public giapi::JmsEpicsBuilder {
public:
	JmsIntEpicsBuilder(BytesMessage *bm);
	virtual ~JmsIntEpicsBuilder();
	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSINTEPICSBUILDER_H_ */
