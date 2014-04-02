#include "JmsEpicsBuilder.h"

namespace giapi {

JmsEpicsBuilder::JmsEpicsBuilder(BytesMessage * bm) {

	_message = bm;

	//Read the common elements to all types of messages.
	//First, the name
	_name = _message->readUTF(); 
	//number of elements.
	_nElements = _message->readInt();

}

JmsEpicsBuilder::~JmsEpicsBuilder() {
}

}
