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

package org.mvel2.optimizers.dynamic;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AbstractOptimizer;
import org.mvel2.optimizers.AccessorOptimizer;

import static java.lang.Thread.currentThread;
import static org.mvel2.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel2.optimizers.OptimizerFactory.getAccessorCompiler;
import static org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer.setMVELClassLoader;

public class DynamicOptimizer extends AbstractOptimizer implements AccessorOptimizer {
  private AccessorOptimizer firstStage = getAccessorCompiler(SAFE_REFLECTIVE);

  private static final Object oLock = new Object();
  private volatile static DynamicClassLoader classLoader;
  public static int tenuringThreshold = 50;
  public static long timeSpan = 100;
  public static int maximumTenure = 1500;
  public static int totalRecycled = 0;
  private static volatile boolean useSafeClassloading = false;

  public void init() {
    _init();
  }

  private static void _init() {
    setMVELClassLoader(classLoader = new DynamicClassLoader(currentThread().getContextClassLoader(), maximumTenure));
  }

  public static void enforceTenureLimit() {
    if (classLoader.isOverloaded()) {
      synchronized (oLock) {
        classLoader.deoptimizeAll();
        totalRecycled = +classLoader.getTotalClasses();
        _init();
      }
    }
  }

  public static final int REGULAR_ACCESSOR = 0;

  public Accessor optimizeAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
                                   VariableResolverFactory factory, boolean rootThisRef, Class ingressType) {
    synchronized (oLock) {
      pCtx.optimizationNotify();
      return classLoader.registerDynamicAccessor(new DynamicGetAccessor(pCtx, property, start, offset, 0,
          firstStage.optimizeAccessor(pCtx, property, start, offset, ctx, thisRef, factory, rootThisRef, ingressType)));
    }
  }

  public static final int SET_ACCESSOR = 1;

  public Accessor optimizeSetAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
                                      VariableResolverFactory factory, boolean rootThisRef, Object value, Class valueType) {

    synchronized (oLock) {
      return classLoader.registerDynamicAccessor(new DynamicSetAccessor(pCtx, property, start, offset,
          firstStage.optimizeSetAccessor(pCtx, property, start, offset, ctx, thisRef, factory, rootThisRef, value, valueType)));
    }
  }

  public static final int COLLECTION = 2;

  public Accessor optimizeCollection(ParserContext pCtx, Object rootObject, Class type, char[] property, int start,
                                     int offset, Object ctx, Object thisRef, VariableResolverFactory factory) {
    synchronized (oLock) {
      return classLoader.registerDynamicAccessor(new DynamicCollectionAccessor(rootObject, type, property, start, offset, 2,
          firstStage.optimizeCollection(pCtx, rootObject, type, property, start, offset, ctx, thisRef, factory)));
    }
  }

  public static final int OBJ_CREATION = 3;

  public Accessor optimizeObjectCreation(ParserContext pCtx, char[] property, int start, int offset,
                                         Object ctx, Object thisRef, VariableResolverFactory factory) {
    synchronized (oLock) {
      return classLoader.registerDynamicAccessor(new DynamicGetAccessor(pCtx, property, start, offset, 3,
          firstStage.optimizeObjectCreation(pCtx, property, start, offset, ctx, thisRef, factory)));
    }
  }

  public static boolean isOverloaded() {
    return classLoader.isOverloaded();
  }

  public Object getResultOptPass() {
    return firstStage.getResultOptPass();
  }

  public Class getEgressType() {
    return firstStage.getEgressType();
  }

  public boolean isLiteralOnly() {
    return firstStage.isLiteralOnly();
  }
}
