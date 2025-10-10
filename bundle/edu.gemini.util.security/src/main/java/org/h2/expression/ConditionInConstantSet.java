/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.ArrayList;
import java.util.HashSet;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

/**
 * Used for optimised IN(...) queries where the contents of the IN list are all
 * constant and of the same type.
 * <p>
 * Checking using a HashSet is has time complexity O(1), instead of O(n) for
 * checking using an array.
 */
public class ConditionInConstantSet extends Condition {

    private Expression left;
    private int queryLevel;
    private final ArrayList<Expression> valueList;
    private final HashSet<Value> valueSet;

    /**
     * Create a new IN(..) condition.
     *
     * @param session the session
     * @param left the expression before IN
     * @param valueList the value list (at least two elements)
     */
    public ConditionInConstantSet(Session session, Expression left, ArrayList<Expression> valueList) {
        this.left = left;
        this.valueList = valueList;
        this.valueSet = new HashSet<Value>(valueList.size());
        for (Expression expression : valueList) {
            this.valueSet.add(expression.getValue(session));
        }
    }

    public Value getValue(Session session) {
        Value leftVal = left.getValue(session);
        if (leftVal == ValueNull.INSTANCE) {
            return leftVal;
        }
        boolean setHasNull = valueSet.contains(ValueNull.INSTANCE);
        Value firstRightVal = valueSet.iterator().next();
        leftVal = leftVal.convertTo(firstRightVal.getType());
        boolean result = valueSet.contains(leftVal);

        if (!result && setHasNull) {
            return ValueNull.INSTANCE;
        }
        return ValueBoolean.get(result);
    }

    public void mapColumns(ColumnResolver resolver, int level) {
        left.mapColumns(resolver, level);
        this.queryLevel = Math.max(level, this.queryLevel);
    }

    public Expression optimize(Session session) {
        left = left.optimize(session);
        return this;
    }

    public void createIndexConditions(Session session, TableFilter filter) {
        if (!(left instanceof ExpressionColumn)) {
            return;
        }
        ExpressionColumn l = (ExpressionColumn) left;
        if (filter != l.getTableFilter()) {
            return;
        }
        if (session.getDatabase().getSettings().optimizeInList) {
            filter.addIndexCondition(IndexCondition.getInList(l, valueList));
            return;
        }
    }

    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        left.setEvaluatable(tableFilter, b);
    }

    public String getSQL() {
        StatementBuilder buff = new StatementBuilder("(");
        buff.append(left.getSQL()).append(" IN(");
        for (Expression e : valueList) {
            buff.appendExceptFirst(", ");
            buff.append(e.getSQL());
        }
        return buff.append("))").toString();
    }

    public void updateAggregate(Session session) {
        // nothing to do
    }

    public boolean isEverything(ExpressionVisitor visitor) {
        if (!left.isEverything(visitor)) {
            return false;
        }
        switch (visitor.getType()) {
        case ExpressionVisitor.OPTIMIZABLE_MIN_MAX_COUNT_ALL:
        case ExpressionVisitor.DETERMINISTIC:
        case ExpressionVisitor.READONLY:
        case ExpressionVisitor.INDEPENDENT:
        case ExpressionVisitor.EVALUATABLE:
        case ExpressionVisitor.SET_MAX_DATA_MODIFICATION_ID:
        case ExpressionVisitor.NOT_FROM_RESOLVER:
        case ExpressionVisitor.GET_DEPENDENCIES:
        case ExpressionVisitor.QUERY_COMPARABLE:
        case ExpressionVisitor.GET_COLUMNS:
            return true;
        default:
            throw DbException.throwInternalError("type=" + visitor.getType());
        }
    }

    public int getCost() {
        int cost = left.getCost();
        return cost;
    }

}
