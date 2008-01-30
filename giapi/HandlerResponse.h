#ifndef HANDLERRESPONSE_H_
#define HANDLERRESPONSE_H_
#include <tr1/memory>

namespace giapi
{
class HandlerResponse
{
	typedef enum {
		ACCEPTED,
		STARTED,
		COMPLETED,
		ERROR
	} Response;
public:
	HandlerResponse();
	virtual ~HandlerResponse();
	
	const Response getResponse() const;
	const char* getMessage() const;
	
};

typedef std::tr1::shared_ptr<HandlerResponse> pHandlerResponse;

}

#endif /*HANDLERRESPONSE_H_*/
