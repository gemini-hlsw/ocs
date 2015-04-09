#ifndef JMSSTRINGEPICSBUILDER_H_
#define JMSSTRINGEPICSBUILDER_H_

#include <string.h>
#include <cstdlib>
#include "JmsEpicsBuilder.h"

namespace giapi {
/**
 * An EPICS Builder to construct EPICS status item containing String values
 */

class JmsStringEpicsBuilder: public giapi::JmsEpicsBuilder {
public:
	JmsStringEpicsBuilder(BytesMessage *msg);
	virtual ~JmsStringEpicsBuilder();
	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSSTRINGEPICSBUILDER_H_ */
