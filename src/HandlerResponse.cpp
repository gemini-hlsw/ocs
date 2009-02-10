#include "giapi/HandlerResponse.h"

namespace giapi {

pHandlerResponse HandlerResponse::create(const Response response) {
	pHandlerResponse result(new HandlerResponse(response));
	return result;
}


pHandlerResponse HandlerResponse::createError(const std::string & msg) {
	pHandlerResponse response(new HandlerResponse(msg));
	return response;
}

HandlerResponse::HandlerResponse(const Response response) {
	_response = response;
}



HandlerResponse::HandlerResponse(const std::string & msg) {
	_response = HandlerResponse::ERROR;
	_message = msg;
}


HandlerResponse::~HandlerResponse() {

}

const std::string HandlerResponse::getMessage() const {
	return _message;
}

const HandlerResponse::Response HandlerResponse::getResponse() const {
	return _response;
}

}
