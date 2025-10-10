/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.bnf;

import java.util.HashMap;

import org.h2.util.StringUtils;

/**
 * A single terminal rule in a BNF object.
 */
class RuleElement implements Rule {

    private final boolean keyword;
    private final String name;
    private Rule link;
    private final int type;

    RuleElement(String name, String topic) {
        this.name = name;
        this.keyword = name.length() == 1 || name.equals(StringUtils.toUpperEnglish(name));
        topic = StringUtils.toLowerEnglish(topic);
        this.type = topic.startsWith("function") ? Sentence.FUNCTION : Sentence.KEYWORD;
    }

    public void accept(BnfVisitor visitor) {
        visitor.visitRuleElement(keyword, name, link);
    }

    public void setLinks(HashMap<String, RuleHead> ruleMap) {
        if (link != null) {
            link.setLinks(ruleMap);
        }
        if (keyword) {
            return;
        }
        String test = Bnf.getRuleMapKey(name);
        for (int i = 0; i < test.length(); i++) {
            String t = test.substring(i);
            RuleHead r = ruleMap.get(t);
            if (r != null) {
                link = r.getRule();
                return;
            }
        }
        throw new AssertionError("Unknown " + name + "/" + test);
    }

    public boolean autoComplete(Sentence sentence) {
        sentence.stopIfRequired();
        if (keyword) {
            String query = sentence.getQuery();
            String q = query.trim();
            String up = sentence.getQueryUpper().trim();
            if (up.startsWith(name)) {
                query = query.substring(name.length());
                while (!"_".equals(name) && query.length() > 0 && Character.isSpaceChar(query.charAt(0))) {
                    query = query.substring(1);
                }
                sentence.setQuery(query);
                return true;
            } else if (q.length() == 0 || name.startsWith(up)) {
                if (q.length() < name.length()) {
                    sentence.add(name, name.substring(q.length()), type);
                }
            }
            return false;
        }
        return link.autoComplete(sentence);
    }

}
