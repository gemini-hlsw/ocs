/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.util.ArrayList;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.index.IndexCondition;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.util.StatementBuilder;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueNull;

/**
 * An 'in' condition with a list of values, as in WHERE NAME IN(...)
 */
public class ConditionIn extends Condition {

    private final Database database;
    private Expression left;
    private final ArrayList<Expression> valueList;
    private int queryLevel;

    /**
     * Create a new IN(..) condition.
     *
     * @param database the database
     * @param left the expression before IN
     * @param values the value list (at least one element)
     */
    public ConditionIn(Database database, Expression left, ArrayList<Expression> values) {
        this.database = database;
        this.left = left;
        this.valueList = values;
    }

    public Value getValue(Session session) {
        Value l = left.getValue(session);
        if (l == ValueNull.INSTANCE) {
            return l;
        }
        boolean result = false;
        boolean hasNull = false;
        for (Expression e : valueList) {
            Value r = e.getValue(session);
            if (r == ValueNull.INSTANCE) {
                hasNull = true;
            } else {
                r = r.convertTo(l.getType());
                result = Comparison.compareNotNull(database, l, r, Comparison.EQUAL);
                if (result) {
                    break;
                }
            }
        }
        if (!result && hasNull) {
            return ValueNull.INSTANCE;
        }
        return ValueBoolean.get(result);
    }

    public void mapColumns(ColumnResolver resolver, int level) {
        left.mapColumns(resolver, level);
        for (Expression e : valueList) {
            e.mapColumns(resolver, level);
        }
        this.queryLevel = Math.max(level, this.queryLevel);
    }

    public Expression optimize(Session session) {
        left = left.optimize(session);
        boolean constant = left.isConstant();
        if (constant && left == ValueExpression.getNull()) {
            return left;
        }
        boolean allValuesConstant = true;
        boolean allValuesSameType = true;
        int size = valueList.size();
        Expression lastExpr = null;
        for (int i = 0; i < size; i++) {
            Expression e = valueList.get(i);
            e = e.optimize(session);
            if (allValuesConstant && !e.isConstant()) {
                allValuesConstant = false;
            }
            valueList.set(i, e);
            if (lastExpr != null && lastExpr.getType() != e.getType()) {
                allValuesSameType = false;
            }
            lastExpr = e;
        }
        if (constant && allValuesConstant) {
            return ValueExpression.get(getValue(session));
        }
        if (size == 1) {
            Expression right = valueList.get(0);
            Expression expr = new Comparison(session, Comparison.EQUAL, left, right);
            expr = expr.optimize(session);
            return expr;
        }
        if (allValuesConstant && allValuesSameType) {
            Expression expr = new ConditionInConstantSet(session, left, valueList);
            expr = expr.optimize(session);
            return expr;
        }
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
            ExpressionVisitor visitor = ExpressionVisitor.getNotFromResolverVisitor(filter);
            for (Expression e : valueList) {
                if (!e.isEverything(visitor)) {
                    return;
                }
            }
            filter.addIndexCondition(IndexCondition.getInList(l, valueList));
            return;
        }
    }

    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        left.setEvaluatable(tableFilter, b);
        for (Expression e : valueList) {
            e.setEvaluatable(tableFilter, b);
        }
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
        left.updateAggregate(session);
        for (Expression e : valueList) {
            e.updateAggregate(session);
        }
    }

    public boolean isEverything(ExpressionVisitor visitor) {
        if (!left.isEverything(visitor)) {
            return false;
        }
        return areAllValues(visitor);
    }

    private boolean areAllValues(ExpressionVisitor visitor) {
        for (Expression e : valueList) {
            if (!e.isEverything(visitor)) {
                return false;
            }
        }
        return true;
    }

    public int getCost() {
        int cost = left.getCost();
        for (Expression e : valueList) {
            cost += e.getCost();
        }
        return cost;
    }

    /**
     * Add an additional element if possible. Example: given two conditions
     * A IN(1, 2) OR A=3, the constant 3 is added: A IN(1, 2, 3).
     *
     * @param other the second condition
     * @return null if the condition was not added, or the new condition
     */
    Expression getAdditional(Comparison other) {
        Expression add = other.getIfEquals(left);
        if (add != null) {
            valueList.add(add);
            return this;
        }
        return null;
    }
}
