/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.mvel2.optimizers.impl.refl.nodes;

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

import java.lang.reflect.Constructor;

import static org.mvel2.DataConversion.convert;

public class ConstructorAccessor extends BaseAccessor {
  private Constructor constructor;
  private Class[] parmTypes;
  private ExecutableStatement[] parms;
  private int length;
  private boolean coercionNeeded = false;

  public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    try {
      if (!coercionNeeded) {
        try {
          if (nextNode != null) {
            return nextNode.getValue(constructor.newInstance(executeAll(elCtx, variableFactory)), elCtx, variableFactory);
          }
          else {
            return constructor.newInstance(executeAll(elCtx, variableFactory));
          }
        }
        catch (IllegalArgumentException e) {
          coercionNeeded = true;
          return getValue(ctx, elCtx, variableFactory);
        }

      }
      else {
        if (nextNode != null) {
          return nextNode.getValue(constructor.newInstance(executeAndCoerce(parmTypes, elCtx, variableFactory)),
              elCtx, variableFactory);
        }
        else {
          return constructor.newInstance(executeAndCoerce(parmTypes, elCtx, variableFactory));
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException("cannot construct object", e);
    }
  }

  public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
    return null;
  }

  private Object[] executeAll(Object ctx, VariableResolverFactory vars) {
    if (length == 0) return GetterAccessor.EMPTY;

    Object[] vals = new Object[length];
    for (int i = 0; i < length; i++) {
      vals[i] = parms[i].getValue(ctx, vars);
    }
    return vals;
  }

  private Object[] executeAndCoerce(Class[] target, Object elCtx, VariableResolverFactory vars) {
    Object[] values = new Object[length];
    for (int i = 0; i < length; i++) {
      //noinspection unchecked
      values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
    }
    return values;
  }

  public ConstructorAccessor(Constructor constructor, ExecutableStatement[] parms) {
    this.constructor = constructor;
    this.length = (this.parmTypes = constructor.getParameterTypes()).length;
    this.parms = parms;
  }

  public Class getKnownEgressType() {
    return constructor.getClass();
  }

  public Constructor getConstructor() {
    return constructor;
  }

  public ExecutableStatement[] getParameters() {
    return parms;
  }

  public Class[] getParameterTypes() {
    return parmTypes;
  }
}
