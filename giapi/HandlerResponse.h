#ifndef HANDLERRESPONSE_H_
#define HANDLERRESPONSE_H_

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

}

#endif /*HANDLERRESPONSE_H_*/
