/*
 * StringUtil.cpp
 *
 *  Created on: Feb 17, 2010
 *      Author: anunez
 */

#include "StringUtil.h"

namespace giapi {

namespace util {

StringUtil::StringUtil() {
}

StringUtil::~StringUtil() {
}

bool StringUtil::isEmpty(const std::string &str) {

	/* Create a copy of the string, to clean it up */
	std::string myString = str;
	/*
	 * Remove all the whitespaces from it
	 */
	myString.erase(myString.find_last_not_of(" \t\n")+1);

	/* If we end up with an empty string, we return true */
	return myString.empty();

}

}

}
