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
package org.mvel.optimizers.impl.asm;

import org.mvel.ASTIterator;
import org.mvel.ASTLinkedList;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.Optimizer;

public class ASMOptimizer implements Optimizer {


    public ASTIterator optimize(ASTLinkedList tokens, Object ctx, VariableResolverFactory variableFactory) {
        return null;
    }

    public String getName() {
        return "ASM";
    }
}
