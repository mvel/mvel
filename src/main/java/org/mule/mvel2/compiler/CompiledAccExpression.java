/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mule.mvel2.compiler;

import static org.mule.mvel2.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.optimizers.OptimizerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CompiledAccExpression implements ExecutableStatement, Serializable {
  private char[] expression;
  private int start;
  private int offset;

  // Should this be thread safe? Original version didn't have the accessor as volatile nor synchronized access to it.
  private transient Map<Class, Accessor> accessorsByIngressType;
  private ParserContext context;
  private Class ingressType;

  public CompiledAccExpression(char[] expression, Class ingressType, ParserContext context) {
    this(expression, 0, expression.length, ingressType, context);
  }

  public CompiledAccExpression(char[] expression, int start, int offset, Class ingressType, ParserContext context) {
    this.expression = expression;
    this.start = start;
    this.offset = offset;

    this.accessorsByIngressType = new HashMap<Class, Accessor>(4, 1.0f);
    this.context = context;
    this.ingressType = ingressType != null ? ingressType : Object.class;
  }

  public Object setValue(final Object ctx, final Object elCtx, final VariableResolverFactory vrf, final Object value) {
    Class valueIngressType;
    if (ingressType == Object.class && value != null) {
      valueIngressType = value.getClass();
    }
    else {
      valueIngressType = ingressType;
    }
    Accessor accessor = accessorsByIngressType.get(valueIngressType);
    if (accessor == null) {
      try {
        accessor = getThreadAccessorOptimizer().optimizeSetAccessor(context, expression, 0, expression.length, ctx, ctx, vrf, false, value, valueIngressType);
        accessorsByIngressType.put(valueIngressType, accessor);
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }
    else {
      accessor.setValue(ctx, elCtx, vrf, value);
    }
    return value;
  }

  public Object getValue(final Object staticContext, final VariableResolverFactory factory) {
    Accessor accessor = accessorsByIngressType.get(ingressType);
    if (accessor == null) {
      try {
        accessor = getThreadAccessorOptimizer().optimizeAccessor(context, expression, 0, expression.length, staticContext, staticContext, factory, false, ingressType);
        accessorsByIngressType.put(ingressType, accessor);
        return getValue(staticContext, factory);
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }
    else {
      return accessor.getValue(factory, staticContext, factory);
    }
  }

  public void setKnownIngressType(Class type) {
    this.ingressType = type;
  }

  public void setKnownEgressType(Class type) {

  }

  public Class getKnownIngressType() {
    return ingressType;
  }

  public Class getKnownEgressType() {
    return null;
  }

  public boolean isConvertableIngressEgress() {
    return false;
  }

  public void computeTypeConversionRule() {
  }

  public boolean intOptimized() {
    return false;
  }

  public boolean isLiteralOnly() {
    return false;
  }

  public Object getValue(final Object ctx, final Object elCtx, final VariableResolverFactory variableFactory) {
    Accessor accessor = accessorsByIngressType.get(ingressType);
    if (accessor == null) {
      try {
        accessor = getThreadAccessorOptimizer().optimizeAccessor(context, expression, start, offset, ctx, elCtx, variableFactory, false, ingressType);
        accessorsByIngressType.put(ingressType, accessor);
        return getValue(ctx, elCtx, variableFactory);
      }
      finally {
        OptimizerFactory.clearThreadAccessorOptimizer();
      }
    }
    else {
      return accessor.getValue(ctx, elCtx, variableFactory);
    }
  }

  public Accessor getAccessor() {
    return isEmptyStatement() ? null : accessorsByIngressType.values().iterator().next();
  }

  public boolean isEmptyStatement() {
    return accessorsByIngressType.isEmpty();
  }

  public boolean isExplicitCast() {
    return false;
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    accessorsByIngressType = new HashMap<Class, Accessor>(4, 1.0f);
  }
}
