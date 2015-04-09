#ifndef JMSSHORTEPICSBUILDER_H_
#define JMSSHORTEPICSBUILDER_H_

#include <string.h>
#include "JmsEpicsBuilder.h"

namespace giapi {
/**
 * An EPICS Builder to construct EPICS status item containing Short values
 */

class JmsShortEpicsBuilder: public JmsEpicsBuilder {
public:
	JmsShortEpicsBuilder(BytesMessage * msg);
	virtual ~JmsShortEpicsBuilder();
	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSSHORTEPICSBUILDER_H_ */
