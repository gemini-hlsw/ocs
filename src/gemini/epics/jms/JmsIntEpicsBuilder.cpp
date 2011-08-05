#include "JmsIntEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>


namespace giapi {

JmsIntEpicsBuilder::JmsIntEpicsBuilder(BytesMessage *bm) : JmsEpicsBuilder(bm) {

}

JmsIntEpicsBuilder::~JmsIntEpicsBuilder() {

}

pEpicsStatusItem JmsIntEpicsBuilder::getEpicsStatusItem() {

	int size = _nElements * sizeof(int);
	char * data = new char[_nElements];
	for (int i = 0; i < _nElements; i++) {
		int val = _message->readInt();
		memcpy(data + i * sizeof(int), &val, sizeof(int));
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::INT,
			_nElements, data, size);
	//destroy the local memory
	delete[] data;
	return item;
}

}
