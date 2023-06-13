/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.HashMap;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

import static org.mvel2.MVEL.executeExpression;

public class MvelArrayTest extends AbstractTest {

    private final String biglistTestScript =
            "var list = [];\n" +
            "list.add(1);\n" +
            "list.add(2);\n" +
            "list.add(3);\n" +
            "list.add(null);\n" + // null!
            "list.add(5);\n" +
            "list.add(6);\n" +
            "list.add(7);\n" +
            "list.add(8);\n" +
            "list.add(9);\n" +
            "list.add(10);\n" +
            "list.add(11);\n" +
            "list.add(12);\n" +
            "java.util.Collections.replaceAll( list, null, 25 );\n" + // replace nulls, with a big value
            "java.util.Collections.max( list );\n";  // return

    public void test1() {
        int actual = (Integer) eval(biglistTestScript);
        assertEquals( actual, 25);
    }

    public void testAsmOptimizerDiesWithBigList() {

        OptimizerFactory.setDefaultOptimizer("ASM");
        Serializable compileExpression = MVEL.compileExpression( biglistTestScript );
        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap());

        int actual = (Integer) executeExpression(compileExpression, factory);
        assertEquals( actual, 25);
    }

    public void testReflectiveOptimizerWorksFine() {

        OptimizerFactory.setDefaultOptimizer("reflective");
        Serializable compileExpression = MVEL.compileExpression( biglistTestScript );
        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap());

        int actual = (Integer) executeExpression(compileExpression, factory);
        assertEquals( actual, 25);
    }

    public void testDynamicOptimizerWorksFine() {

        OptimizerFactory.setDefaultOptimizer("dynamic");
        Serializable compileExpression = MVEL.compileExpression( biglistTestScript );
        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap());

        int actual = (Integer) executeExpression(compileExpression, factory);
        assertEquals( actual, 25);
    }

    private final String smallListTestScript =
            "list = [];\n" +
            "list.add(1);\n" +
            "list.add(2);\n" +
            "list.add(3);\n" +
            "list.add(null);\n" + // null!
            "list.add(5);\n" +
            "list.add(6);\n" +
            "java.util.Collections.replaceAll( list, null, 25 );\n" + // replace nulls, with a big value
            "java.util.Collections.max( list );\n";  // return

    public void testAsmOptimizerWithSmallListIsFine() {

        OptimizerFactory.setDefaultOptimizer("ASM");
        Serializable compileExpression = MVEL.compileExpression( smallListTestScript );
        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap());

        int actual = (Integer) executeExpression(compileExpression, factory);
        assertEquals( actual, 25);
    }
}