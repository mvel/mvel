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

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;

public class PrimitiveTypesTest extends AbstractTest {
    public static class FactWithFloat {

        private final float floatValue;
        private final Float floatObjectValue;

        public FactWithFloat(final float floatValue) {
            this.floatValue = floatValue;
            this.floatObjectValue = floatValue;
        }

        public float getFloatValue() {
            return floatValue;
        }

        public Float getFloatObjectValue() {
            return floatObjectValue;
        }
    }

    public void testFloatPrimitive() {
        ParserConfiguration conf = new ParserConfiguration();
        conf.addImport( FactWithFloat.class );
        ParserContext pctx = new ParserContext( conf );
        pctx.setStrictTypeEnforcement(true);
        pctx.setStrongTyping(true);
        pctx.addInput("this", FactWithFloat.class);
        boolean result = ( Boolean ) MVEL.executeExpression(MVEL.compileExpression("floatValue == 15.1", pctx), new FactWithFloat(15.1f));
        assertTrue( result );
    }

    public void testFloat() {
        ParserConfiguration conf = new ParserConfiguration();
        conf.addImport( FactWithFloat.class );
        ParserContext pctx = new ParserContext( conf );
        pctx.setStrictTypeEnforcement(true);
        pctx.setStrongTyping(true);
        pctx.addInput("this", FactWithFloat.class);
        boolean result = ( Boolean ) MVEL.executeExpression(MVEL.compileExpression("floatObjectValue == 15.1", pctx), new FactWithFloat(15.1f));
        assertTrue( result );
    }
}
