package com.ifttt.abstracts.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ifttt.enums.OperatorEnum;
import com.ifttt.utils.Evaluator;
import com.ifttt.abstracts.RuleEngine;
import com.ifttt.annotations.Fact;
import com.ifttt.enums.BooleanJoinEnum;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author praveenkamath
 **/
public class RuleEngineBooleanImpl extends RuleEngine {

    private JsonNode rule;

    private Object object;

    private Map<String, Object> factMap;

    @Override
    public RuleEngine runThis(JsonNode rule) throws Exception {
        if(compileThis()) {
            this.rule   =   rule;
            return this;
        } throw new Exception("Rule compilation error!");
    }

    private static boolean compileThis() {
        //TODO add compilation logic
        return true;
    }

    @Override
    public RuleEngineBooleanImpl against(Object object) throws Exception {
        this.object     =   object;
        if(object instanceof Map) {
            this.factMap = (Map<String, Object>) object;
        } else {
            constructAnnotationFieldMap();
        } return this;
    }

    private void constructAnnotationFieldMap() throws Exception {
        factMap =   new HashMap<>();
        for(Field field: object.getClass().getFields()) {
            Annotation annotation   =  field.getAnnotation(Fact.class);
            if(annotation != null && field.get(object) != null) {
                factMap.put((field.getAnnotation(Fact.class)).value(), field.get(object));
            }
        }
    }

    @Override
    public Boolean execute() throws Exception {
        return exec(rule);
    }

    private boolean exec(JsonNode rule) throws Exception {
        JsonNode anyRule =   rule.get("any");
        JsonNode allRule =   rule.get("all");
        boolean any = false;
        boolean all = true;
        if(anyRule != null) {
            for(JsonNode  object : anyRule) {
                any = any || recursivelyApply(object, BooleanJoinEnum.ANY);
            } return any;
        } else if(allRule != null) {
            for(JsonNode object : allRule) {
                all = all && recursivelyApply(object, BooleanJoinEnum.ALL);
            } return all;
        } return false;
    }

    private boolean recursivelyApply(JsonNode rule, BooleanJoinEnum join) throws Exception {
        if(rule.get("any") == null && rule.get("all") == null) {
            return join    ==  BooleanJoinEnum.ANY ? any(rule) : all(rule);
        } return exec (rule);
    }

    private boolean any(JsonNode rule) throws Exception {
        Object factVal  = factMap.get(rule.get("fact").asText());
        return factVal != null && (boolean) eval(rule);
    }

    private boolean all(JsonNode rule) throws Exception {
        Object factVal = factMap.get(rule.get("fact").asText());
        return factVal != null && (boolean) eval(rule);
    }

    private Object eval(JsonNode rule) throws Exception {
        return Evaluator.eval(OperatorEnum.valueOf(rule.get("operator").asText()), factMap.get(rule.get("fact").asText()), rule);
    }
}
