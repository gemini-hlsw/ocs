#ifndef GIAPIEXCEPTION_H_
#define GIAPIEXCEPTION_H_

#include <stdexcept>
#include <cstdarg>
#include <string>
/**
 * This file contains definition of exceptions in the GIAPI
 */
namespace giapi {
/**
 * Base class for exceptions within the GIAPI framework
 */
class GiapiException: public std::exception {

public:

	/**
	 * Constructor
	 */
	GiapiException() throw();

	/**
	 * Copy Constructor
	 */
	GiapiException(const GiapiException & ex) throw();


	/**
	 * Constructor that takes as argument the message
	 * that describes this exception
	 */
	GiapiException(const std::string & message) throw();

	virtual ~GiapiException() throw();

	/**
	 * Gets the message for this exception
	 *
	 * @return text message for this exception
	 */
	virtual std::string getMessage() const {
		return _message;
	}

	/**
	 * Implement method from std::exception
	 * @return the const char* of <code>getMessage()</code>.
	 */
	virtual const char* what() const throw () {
		return _message.c_str();
	}

	/**
	 * Sets the cause for this exception.
	 * @param msg the string for the cause of the exception.
	 */
	virtual void setMessage(const std::string & msg) {
		_message = msg;
	}

	/**
	 * Sets the cause for this exception.
	 * @param msg the format string for the message
	 * @param variable - parameters to format into the string
	 */
	virtual void setMessage(const char * msg, ...);

protected:
	std::string _message;

	virtual void buildMessage(const char* format, va_list& vargs);
};

/**
 * A communication exception. These exceptions are raised when
 * some communication between the GIAPI library and the Gemini
 * Master Process fails.
 */
class CommunicationException: public GiapiException {
public:

	/**
	 * Default constructor
	 */
	CommunicationException() throw();

	/**
	 * Constructor that will take as argument the message
	 * used to describe the reason why the exception was thrown
	 */
	CommunicationException(const std::string& message) throw();

};


/**
 * A GMP exception. Raised when a problem trying to contact the
 * Gemini Master Process occurs.
 */
class GmpException: public CommunicationException {

public:
	/**
	 * Constructor
	 */
	GmpException() throw ();

	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the exception was thrown
	 */
	GmpException(const std::string & message) throw ();
};


/**
 * A post exception. A post exception is raised when some condition
 * prevents a post operation to complete.
 */
class PostException: public CommunicationException {
public:
	/**
	 * Default constructor
	 */
	PostException() throw();
	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the exception was thrown
	 */
	PostException(const std::string& message) throw();
};

/**
 * An Invalid operation exception. Thrown if the requested
 * operation is not valid in the current context
 */
class InvalidOperation: public GiapiException {
public:
	/**
	 * Default constructor
	 */
	InvalidOperation() throw ();
	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the operation is invalid
	 */
	InvalidOperation(const std::string& message) throw();

};


/**
 * A timeout exception. Thrown when an operation exceeds the
 * maximum expected time for it to complete.
 */
class TimeoutException: public GiapiException {
public:
	/**
	 * Default constructor
	 */
	TimeoutException() throw();
	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the timeout happened
	 */
	TimeoutException(const std::string &message) throw();

};


}

#endif /*GIAPIEXCEPTION_H_*/
