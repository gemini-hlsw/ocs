#ifndef GIAPIEXCEPTION_H_
#define GIAPIEXCEPTION_H_

#include <stdexcept>

/**
 * This file contains definition of exceptions in the GIAPI
 */
namespace giapi {

	/**
	 * A post exception. A post exception is raised when some condition
	 * prevents a StatusSender to complete. 
	 */
	class PostException : public std::runtime_error {
	public:
		/**
		 * Default constructor
		 */
		PostException();
		/**
		 * Constructor that will take as an argument a message used
		 * to describe the reason why the exception was thrown
		 */
		PostException(const char * message);
	};

	class InvalidOperation : public std::logic_error {
		/**
		 * Default constructor
		 */
		InvalidOperation();
		/**
		 * Constructor that will take as an argument a message used
		 * to describe the reason why the operation is invalid
		 */
		InvalidOperation(const char * message);

	};

}

#endif /*GIAPIEXCEPTION_H_*/
