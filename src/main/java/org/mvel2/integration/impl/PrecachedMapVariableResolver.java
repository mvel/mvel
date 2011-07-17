package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;

import java.util.Map;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;

public class PrecachedMapVariableResolver implements VariableResolver {
  private String name;
  private Class<?> knownType;
  private Map.Entry entry;

  public PrecachedMapVariableResolver(Map.Entry entry, String name) {
    this.entry = entry;
    this.name = name;
  }

  public PrecachedMapVariableResolver(Map.Entry entry, String name, Class knownType) {
    this.name = name;
    this.knownType = knownType;
    this.entry = entry;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStaticType(Class knownType) {
    this.knownType = knownType;
  }


  public String getName() {
    return name;
  }

  public Class getType() {
    return knownType;
  }

  public void setValue(Object value) {
    if (knownType != null && value != null && value.getClass() != knownType) {
      Class t = value.getClass();
      if (!canConvert(knownType, t)) {
        throw new RuntimeException("cannot assign " + value.getClass().getName() + " to type: "
            + knownType.getName());
      }
      try {
        value = convert(value, knownType);
      }
      catch (Exception e) {
        throw new RuntimeException("cannot convert value of " + value.getClass().getName()
            + " to: " + knownType.getName());
      }
    }

    //noinspection unchecked
    entry.setValue(value);
  }

  public Object getValue() {
    return entry.getValue();
  }

  public int getFlags() {
    return 0;
  }
}
