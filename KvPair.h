#ifndef KVPAIR_H_
#define KVPAIR_H_
#include <boost/any.hpp>

#include <typeinfo>
/**
 * A class that holds a key associated to a value. 
 */
namespace giapi {

class KvPair {
private:

protected:
	KvPair(const char *name);
	boost::any _value;
	const char * _name;

public:
	virtual ~KvPair();
	const char* getName() const;
	/**
	 * Returns the type information associated to the 
	 * value stored in this pair. The type information
	 * is defined based on the last value stored in the object. 
	 * If the value has not been set, the type of the stored 
	 * item is void. 
	 */
	const std::type_info & getType() const;

	/**
	 * Set the value to the given int. Previous value is replaced,
	 * no matter what type it was. Subclasses can specialize this 
	 * behavior providing extra validations.
	 * 
	 * @param value the new integer value.
	 * 
	 * @return status::OK. Subclasses can return different
	 *         values. See their specific documentation
	 */
	virtual int setValueAsInt(int value);

	/**
	 * Set the value to the given string. Previous value is replaced,
	 * no matter what type it was. Subclasses can specialize this 
	 * behavior providing extra validations.
	 * 
	 * @param value the new string value.
	 * 
	 * @return status::OK. Subclasses can return different
	 *         values. See their specific documentation
	 */
	virtual int setValueAsString(const char * value);

	/**
	 * Set the value to the given double. Previous value is replaced,
	 * no matter what type it was. Subclasses can specialize this 
	 * behavior providing extra validations.
	 * 
	 * @param value the new double value.
	 * 
	 * @return status::OK. Subclasses can return different
	 *         values. See their specific documentation
	 */
	virtual int setValueAsDouble(double value);

	/**
	 * Returns the value stored as an integer.
	 *  
	 * @return integer stored
	 * 
	 * @throws bad_cast exception if the stored item is not an integer
	 */
	int getValueAsInt() const;

	/**
	 * Returns the value stored as a double.
	 *  
	 * @return double stored
	 * 
	 * @throws bad_cast exception if the stored item is not a double
	 */
	double getValueAsDouble() const;

	/**
	 * Returns the value stored as a string.
	 *  
	 * @return string stored
	 * 
	 * @throws bad_cast exception if the stored item is not a string
	 */
	const char * getValueAsString() const;

};

}

#endif /*KVPAIR_H_*/
