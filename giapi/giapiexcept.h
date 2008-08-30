#ifndef GIAPIEXCEPTION_H_
#define GIAPIEXCEPTION_H_

#include <stdexcept>
#include <string>
/**
 * This file contains definition of exceptions in the GIAPI
 */
namespace giapi {
/**
 * Base class for exceptions within the GIAPI framework
 */
class GiapiException : public std::exception {

public:

	/**
	 * Constructor
	 */
	GiapiException() throw();

	/**
	 * Copy Constructor
	 */
	GiapiException(const GiapiException & ex) throw();
	
	
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

protected:
	std::string _message;
};

/**
 * A post exception. A post exception is raised when some condition
 * prevents a StatusSender to complete. 
 */
class PostException : public GiapiException {
public:
	/**
	 * Default constructor
	 */
	PostException();
	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the exception was thrown
	 */
	PostException(const std::string& message);
};

/**
 * An Invalid operation exception. Thrown if the requested 
 * operation is not valid in the current context
 */
class InvalidOperation : public std::logic_error {
public:
	/**
	 * Default constructor
	 */
	InvalidOperation();
	/**
	 * Constructor that will take as an argument a message used
	 * to describe the reason why the operation is invalid
	 */
	InvalidOperation(const std::string& message);

};

}

#endif /*GIAPIEXCEPTION_H_*/
