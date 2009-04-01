/*
 * JmsByteEpicsBuilder.cpp
 *
 *  Created on: Mar 31, 2009
 *      Author: anunez
 */

#include "JmsByteEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsByteEpicsBuilder::JmsByteEpicsBuilder(BytesMessage *bm) :
	JmsEpicsBuilder(bm) {

}

JmsByteEpicsBuilder::~JmsByteEpicsBuilder() {
}

pEpicsStatusItem JmsByteEpicsBuilder::getEpicsStatusItem() {

	int size = _nElements * sizeof(unsigned char);
	unsigned char * data = (unsigned char *) malloc(size);

	_message->readBytes(data, size);

	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::BYTE,
			_nElements, data, size);
	//destroy the local memory
	delete data;
	return item;
}


}
