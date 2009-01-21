/*
 * StatusVisitor.h
 *
 *  Created on: Jan 20, 2009
 *      Author: anunez
 */

#ifndef STATUSVISITOR_H_
#define STATUSVISITOR_H_

#include <exception>
namespace giapi {

//forward declarations.
class StatusItem;
class AlarmStatusItem;
class HealthStatusItem;

/**
 * Visitor to apply the visitor pattern over Status Items, allowing
 *  to perform different operations over them.
 */
class StatusVisitor {
public:
	virtual ~StatusVisitor();

	/**
	 * Defines an operation over the StatusItem
	 */
	virtual void visitStatusItem(StatusItem * item) throw (std::exception) = 0;

	/**
	 * Defines an operation over the Alarms
	 */
	virtual void visitAlarmItem(AlarmStatusItem * item) throw (std::exception) = 0;

	/**
	 * Defines an operation over Health Status item
	 */
	virtual void visitHealthItem(HealthStatusItem * item) throw (std::exception) = 0;

protected:
	StatusVisitor();

};

}

#endif /* STATUSVISITOR_H_ */
