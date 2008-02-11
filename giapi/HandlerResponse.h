#ifndef HANDLERRESPONSE_H_
#define HANDLERRESPONSE_H_
#include <tr1/memory>

namespace giapi {
/**
 * Handler response. Contains an enumerated response type 
 * and a message when an error is produced. 
 */
class HandlerResponse {
public:
	/**
	 * Definition of the different response types
	 */
	 enum Response {
		/**
		 * Preset Accepted.
		 */
		ACCEPTED,
		/**
		 * Actions started
		 */
		STARTED,
		/**
		 * Actions completed
		 */
		COMPLETED,
		/**
		 * Request ended with error.
		 */
		ERROR
	};

	/**
	 * Constructor. Takes the response type as the argument
	 */
	HandlerResponse(const Response response);

	/**
	 * Constructor for error Handler response.
	 * The Response type is set to ERROR and the message is 
	 * stored.  
	 */
	HandlerResponse(const char * msg);

	/**
	 * Return the response type. 
	 */
	const Response getResponse() const;
	
	/**
	 * Return the message associated to this handler
	 * response. If the response type is not ERROR, 
	 * the return value is NULL. 
	 */
	const char* getMessage() const;
	
	virtual ~HandlerResponse();

private:
	const char * _message;
	Response _response;

};

/**
 * Definition of a smart pointer to a HandlerResponse instance 
 */
typedef std::tr1::shared_ptr<HandlerResponse> pHandlerResponse;

}

#endif /*HANDLERRESPONSE_H_*/
