#ifndef KVPAIR_H_
#define KVPAIR_H_
#include <boost/any.hpp>
//TODO: Make the boost libraries to be available for the distro. 

#include <typeinfo>
/**
 * 
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
	 */
	const std::type_info & getType() const;

	//setters
	virtual int setValueAsInt(int value);
	virtual int setValueAsString(const char * value);

	//getters. Getters might throw bad_cast exceptions 
	int getValueAsInt() const;
	const char * getValueAsString() const;

};

}

#endif /*KVPAIR_H_*/
