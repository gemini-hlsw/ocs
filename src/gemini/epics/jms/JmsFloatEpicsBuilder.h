#ifndef JMSFLOATEPICSBUILDER_H_
#define JMSFLOATEPICSBUILDER_H_

#include <string.h>
#include "JmsEpicsBuilder.h"

namespace giapi {
/**
 * An EPICS Builder to construct EPICS status item containing Float values
 */

class JmsFloatEpicsBuilder: public giapi::JmsEpicsBuilder {
public:
	JmsFloatEpicsBuilder(BytesMessage * bm);
	virtual ~JmsFloatEpicsBuilder();
	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSFLOATEPICSBUILDER_H_ */
