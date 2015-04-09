
#ifndef JMSDOUBLEEPICSBUILDER_H_
#define JMSDOUBLEEPICSBUILDER_H_

#include "JmsEpicsBuilder.h"
#include <string.h>

namespace giapi {

/**
 * An EPICS Builder to construct EPICS status item containing Double values
 */

class JmsDoubleEpicsBuilder : public JmsEpicsBuilder {
public:
	JmsDoubleEpicsBuilder(BytesMessage * bm);
	virtual ~JmsDoubleEpicsBuilder();

	virtual pEpicsStatusItem getEpicsStatusItem();
};

}

#endif /* JMSDOUBLEEPICSBUILDER_H_ */
