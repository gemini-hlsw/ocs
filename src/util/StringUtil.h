
#ifndef STRINGUTIL_H_
#define STRINGUTIL_H_
#include <string>

namespace giapi {

namespace util {

/**
 * Collection of auxiliary methods to deal with Strings.
 */

class StringUtil {
public:

	/**
	 * Returns true if the given string is empty (zero size, or
	 * just whitespaces)
	 */
	static bool isEmpty(const std::string &str);


	virtual ~StringUtil();
private:
	StringUtil();


};

}
}

#endif /* STRINGUTIL_H_ */
