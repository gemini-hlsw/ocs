#include "JmsShortEpicsBuilder.h"

#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsShortEpicsBuilder::JmsShortEpicsBuilder(BytesMessage *bm) :
	JmsEpicsBuilder(bm){
}

JmsShortEpicsBuilder::~JmsShortEpicsBuilder() {
}

pEpicsStatusItem JmsShortEpicsBuilder::getEpicsStatusItem() {

	int size = _nElements * sizeof(short int);
	char * data = new char[_nElements];
	for (int i = 0; i < _nElements; i++) {
		short int val = _message->readShort();
		memcpy(data + i * sizeof(short int), &val, sizeof(short int));
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::SHORT,
			_nElements, data, size);
	//destroy the local memory
	delete[] data;
	return item;
}


}
