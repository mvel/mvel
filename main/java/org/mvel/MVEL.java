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

package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.OptimizerFactory;

public class MVEL {
    public static final String NAME = "MVEL (MVFLEX Expression Language)";
    public static final String VERSION = "1.2";
    public static final String VERSION_SUB = "beta1";
    public static final String CODENAME = "horizon";
    
    static boolean THREAD_SAFE = Boolean.getBoolean("mvel.threadsafety");
    static boolean USE_OPTIMIZER = Boolean.getBoolean("mvel.optimizer");

    /**
     * Force MVEL to use thread-safe caching.  This can also be specified enivromentally using the
     * <tt>mvflex.expression.threadsafety</tt> system property.
     *
     * @param threadSafe - true enabled thread-safe caching - false disables thread-safety.
     */
    public static void setThreadSafe(boolean threadSafe) {
        THREAD_SAFE = threadSafe;
        PropertyAccessor.configureFactory();
        Interpreter.configureFactory();
        ExpressionParser.configureFactory();
    }

    public static void setUseOptimizer(boolean optimizer) {
        USE_OPTIMIZER = optimizer;
    }

    public static boolean isThreadSafe() {
        return THREAD_SAFE;
    }

    public static boolean useOptimizer() {
        return USE_OPTIMIZER;
    }

    public static Object execute(CompiledExpression compiledStatement, Object rootContext, VariableResolverFactory factory) {
       return null;
    }


    public static ExecutableStatement optimize(TokenIterator tokenIterator, Object rootContext, VariableResolverFactory factory) {
        return OptimizerFactory.getDefaultOptimizer().optimize(tokenIterator, rootContext, factory);
    }

}
