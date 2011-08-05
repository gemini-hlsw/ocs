#include "JmsFloatEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsFloatEpicsBuilder::JmsFloatEpicsBuilder(BytesMessage *bm) :
	JmsEpicsBuilder(bm) {

}

JmsFloatEpicsBuilder::~JmsFloatEpicsBuilder() {

}

pEpicsStatusItem JmsFloatEpicsBuilder::getEpicsStatusItem() {

	int size = _nElements * sizeof(float);
	char * data = new char[_nElements];
	for (int i = 0; i < _nElements; i++) {
		float val = _message->readFloat();
		memcpy(data + i * sizeof(float), &val, sizeof(float));
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::FLOAT,
			_nElements, data, size);
	//destroy the local memory
	delete[] data;
	return item;
}

}
