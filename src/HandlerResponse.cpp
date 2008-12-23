#include "giapi/HandlerResponse.h"

namespace giapi {

pHandlerResponse HandlerResponse::create(const Response response) {
	pHandlerResponse result(new HandlerResponse(response));
	return result;
}


pHandlerResponse HandlerResponse::createError(const char * msg) {
	pHandlerResponse response(new HandlerResponse(msg));
	return response;
}

HandlerResponse::HandlerResponse(const Response response) {
	_response = response;
	_message = NULL;
}



HandlerResponse::HandlerResponse(const char * msg) {
	_response = HandlerResponse::ERROR;
	if (msg != NULL) {
		_message = new char[strlen(msg) + 1];
		strcpy(_message, msg);
	}
}


HandlerResponse::~HandlerResponse() {

	if (_message != NULL) {
		delete[] _message;
	}
}

const char * HandlerResponse::getMessage() const {
	return _message;
}

const HandlerResponse::Response HandlerResponse::getResponse() const {
	return _response;
}

}
