package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;

import static org.mvel2.integration.impl.SimpleSTValueResolver.handleTypeCoercion;
import static org.mvel2.util.ParseTools.isNumeric;

public class IndexVariableResolver implements VariableResolver {
  private int indexPos;
  private Object[] vars;
  private Class type;

  public IndexVariableResolver(int indexPos, Object[] vars) {
    this.indexPos = indexPos;
    this.vars = vars;
    initializeType(vars[indexPos]);
  }

  public String getName() {
    return null;
  }

  public Class getType() {
    return type;
  }

  public void setStaticType(Class type) {
      this.type = type;
  }

  public int getFlags() {
    return 0;
  }

  public Object getValue() {
    return vars[indexPos];
  }

  public void setValue(Object value) {
    initializeType(value); // Initialize type if not yet done

    if (type == Object.class || type == null) {
      // IndexVariableResolver doesn't know the original declared type, so cannot deal with polymorphism
      // Just accept the value
      vars[indexPos] = value;
    } else {
      // Coerce numeric types
      vars[indexPos] = handleTypeCoercion(type, value);
    }
  }

  private void initializeType(Object value) {
    if (type == null && value != null) {
      if (isNumeric(value)) {
        // Use type only when numeric
        type = value.getClass();
      } else {
        // Otherwise, accept any type
        type = Object.class;
      }
    }
  }
}
