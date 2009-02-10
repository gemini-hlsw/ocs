#ifndef HEALTHSTATUSITEM_H_
#define HEALTHSTATUSITEM_H_

#include <status/StatusItem.h>
#include <status/StatusVisitor.h>
#include <giapi/giapi.h>

namespace giapi
{

/**
 * A Health Status Item. A health status item provides a way
 * for systems/subsystems to indicate an overall operational
 * status of themselves.
 *
 * TODO: It is not good that health status item is exposing the
 * public StatusItem API. Probably the base interface for status items
 * need to change to hide the set methods.
 */
class HealthStatusItem : public giapi::StatusItem
{
public:
	HealthStatusItem(const std::string & name);
	virtual ~HealthStatusItem();

	/**
	 * Set the health for this status item.
	 *
	 * @param health The new health state
	 *
	 * @return giapi::status::OK if the health is set correctly
	 *         giapi::status::WARNING if the health was already configured
	 *         with the value defined in <code>health</code>
	 *         giapi::status::ERROR if there is a problem setting the
	 *         health value.
	 *
	 */
	int setHealth(const health::Health health);

	/**
	 * The accept interface for the visitor pattern
	 */
	void accept(StatusVisitor &visitor);
};

}

#endif /*HEALTHSTATUSITEM_H_*/
