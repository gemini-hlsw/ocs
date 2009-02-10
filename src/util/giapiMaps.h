#ifndef GIAPIMAPS_H_
#define GIAPIMAPS_H_

/**
 * Contains a collection of definitions that allow the use of the GIAPI
 * datastructures/types in hash maps.
 */

#include <ext/hash_map>
#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>

using namespace giapi;

//hash_map is an extension of STL widely available on gnu compilers, fortunately
//Will make its namespace visible here.
using namespace __gnu_cxx;

namespace __gnu_cxx {
template<> struct hash<command::Activity> {
	size_t operator()(const command::Activity& x) const {
		return hash<int>()((int)x );
	}
};

template<> struct hash<command::SequenceCommand> {
	size_t operator()(const command::SequenceCommand& x) const {
		return hash<int>()((int)x );
	}
};

template<> struct hash<HandlerResponse::Response> {
	size_t operator()(const HandlerResponse::Response& x) const {
		return hash<int>()((int)x );
	}
};

template<> struct hash<std::string> {
	size_t operator()(const std::string& x) const {
		return hash<const char*>()(x.c_str() );
	}
};
}

namespace giapi {
namespace util {
/**
 * A comparator for strings to be used in the definition of
 * hash_tables
 */
struct eqstr {
	bool operator()(const std::string& s1, const std::string& s2) const {
		return (s1 == s2);
	}
};

}

/**
 * Type definition for the hash_table that will map strings to
 * Action Ids.
 */
typedef hash_map<const std::string, command::ActionId, hash<std::string>, util::eqstr>
		StringActionIdMap;

/**
 * Type definition for the hash_table that will map Responses  to
 * the string that represent them
 */
typedef hash_map<HandlerResponse::Response, std::string> ResponseStringMap;

/**
 * Type definition for the hash_table that will map command Ids to
 * a string
 */
typedef hash_map<command::SequenceCommand, std::string>
		SequenceCommandStringMap;

}

#endif /*GIAPIMAPS_H_*/
