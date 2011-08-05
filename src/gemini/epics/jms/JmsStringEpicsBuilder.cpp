#include "JmsStringEpicsBuilder.h"
#include <gemini/epics/EpicsStatusItemImpl.h>

namespace giapi {

JmsStringEpicsBuilder::JmsStringEpicsBuilder(BytesMessage *bm) : JmsEpicsBuilder(bm) {

}

JmsStringEpicsBuilder::~JmsStringEpicsBuilder() {
}

pEpicsStatusItem JmsStringEpicsBuilder::getEpicsStatusItem() {

	char * data = NULL;//todo: this should be changed to an std::vector to avoid realloc
	char * oldData;
	int size = 0;
	for (int i = 0; i < _nElements; i++) {
		std::string val = _message->readUTF();
		const char * cstr = val.c_str();
		int oldsize = size;
		size = size + strlen(cstr) + 1;
        oldData = data;
		data = (char *)realloc(data, size);
        if (data == NULL) {//if realloc fails
            free(oldData);
            break;
        }
		strcpy(data + oldsize, cstr);
	}
	pEpicsStatusItem item = EpicsStatusItemImpl::create(_name, type::STRING,
			_nElements, data, size);
	//destroy the local memory
	free(data);
	return item;
}

}
