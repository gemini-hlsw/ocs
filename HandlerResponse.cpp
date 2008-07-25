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
	_message = 0;
}



HandlerResponse::HandlerResponse(const char * msg) {
	_response = HandlerResponse::ERROR;
	_message = msg;
}


HandlerResponse::~HandlerResponse() {
}

const char * HandlerResponse::getMessage() const {
	return _message;
}

const HandlerResponse::Response HandlerResponse::getResponse() const {
	return _response;
}

}
