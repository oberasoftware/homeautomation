package com.oberasoftware.home.rules.api;

import com.oberasoftware.home.api.types.VALUE_TYPE;

/**
 * @author Renze de Vries
 */
public class StaticValue implements ResolvableValue {
    private Object value;
    private VALUE_TYPE type;

    public StaticValue(Object value, VALUE_TYPE type) {
        this.value = value;
        this.type = type;
    }

    public StaticValue() {
    }

    public VALUE_TYPE getType() {
        return this.type;
    }

    public void setType(VALUE_TYPE type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "StaticValueImpl{" +
                "value=" + value +
                '}';
    }
}