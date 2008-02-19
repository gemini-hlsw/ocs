#ifndef HANDLERRESPONSE_H_
#define HANDLERRESPONSE_H_
#include <tr1/memory>


namespace giapi {

/**
 * forward declaration
 */
class HandlerResponse;
/**
 * Definition of a smart pointer to a HandlerResponse instance 
 */
typedef std::tr1::shared_ptr<HandlerResponse> pHandlerResponse;

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
	 * Static factory initializer. Takes as an argument
	 * the type of the response
	 */

	static pHandlerResponse create(const Response response);

	/**
	 * Static factory initializer for error HandlerResponse. 
	 * The response type is set to ERROR and the message is 
	 * stored. 
	 */
	static pHandlerResponse create(const char * msg);

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
	/**
	 * Private Constructor. Takes the response type as the argument
	 */
	HandlerResponse(const Response response);

	/**
	 * Private Constructor for error Handler response.
	 * The Response type is set to ERROR and the message is 
	 * stored.  
	 */
	HandlerResponse(const char * msg);
	/**
	 * The error message 
	 */
	const char * _message;
	
	/**
	 * The response type
	 */
	Response _response;

};



}

#endif /*HANDLERRESPONSE_H_*/
