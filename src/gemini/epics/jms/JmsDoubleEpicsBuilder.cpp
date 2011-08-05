#include "JmsDoubleEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsDoubleEpicsBuilder::JmsDoubleEpicsBuilder(BytesMessage *bm) :
	JmsEpicsBuilder(bm) {

}

JmsDoubleEpicsBuilder::~JmsDoubleEpicsBuilder() {
}

pEpicsStatusItem JmsDoubleEpicsBuilder::getEpicsStatusItem() {

	int size = _nElements * sizeof(double);
	char * data = new char[_nElements];
	for (int i = 0; i < _nElements; i++) {
		double val = _message->readDouble();
		memcpy(data + i * sizeof(double), &val, sizeof(double));
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::DOUBLE,
			_nElements, data, size);
	//destroy the local memory
	delete[] data;
	return item;
}

}
