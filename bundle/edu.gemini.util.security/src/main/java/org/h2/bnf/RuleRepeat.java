/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.bnf;

import java.util.HashMap;

/**
 * Represents a loop in a BNF object.
 */
class RuleRepeat implements Rule {

    private final Rule rule;
    private final boolean comma;

    RuleRepeat(Rule rule, boolean comma) {
        this.rule = rule;
        this.comma = comma;
    }

    public void accept(BnfVisitor visitor) {
        visitor.visitRuleRepeat(comma, rule);
    }

    public void setLinks(HashMap<String, RuleHead> ruleMap) {
        // not required, because it's already linked
    }

    public boolean autoComplete(Sentence sentence) {
        sentence.stopIfRequired();
        while (rule.autoComplete(sentence)) {
            // nothing to do
        }
        return true;
    }

}
