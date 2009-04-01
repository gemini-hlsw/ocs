/*
 * JmsStringEpicsBuilder.cpp
 *
 *  Created on: Mar 31, 2009
 *      Author: anunez
 */

#include "JmsStringEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsStringEpicsBuilder::JmsStringEpicsBuilder(BytesMessage *bm) : JmsEpicsBuilder(bm) {
	// TODO Auto-generated constructor stub

}

JmsStringEpicsBuilder::~JmsStringEpicsBuilder() {
	// TODO Auto-generated destructor stub
}

pEpicsStatusItem JmsStringEpicsBuilder::getEpicsStatusItem() {

	char * data = NULL;
	int size = 0;
	for (int i = 0; i < _nElements; i++) {
		std::string val = _message->readUTF();
		const char * cstr = val.c_str();
		int oldsize = size;
		size = size + strlen(cstr) + 1;
		data = (char *)realloc(data, size);
		strcpy(data + oldsize, cstr);
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::STRING,
			_nElements, data, size);
	//destroy the local memory
	delete data;
	return item;
}

}
