/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.mvel3.transpiler;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.expr.Expression;
import org.mvel3.transpiler.ast.TypedExpression;
import org.mvel3.transpiler.context.TranspilerContext;

/* A special case of compiler in that compiles constraints, that is
    every variable can be implicitly a field of the root object
    no LHS
    converted FieldToAccessor prepend a this expr
 */
public class ConstraintTranspiler {

    private final TranspilerContext mvelTranspilerContext;

    public ConstraintTranspiler(TranspilerContext mvelTranspilerContext) {
        this.mvelTranspilerContext = mvelTranspilerContext;
    }

    public TranspiledExpressionResult compileExpression(String mvelExpressionString) {
        ParseResult<Expression> result = mvelTranspilerContext.getParser().parseExpression(mvelExpressionString);
        if (!result.isSuccessful()) {
            throw new RuntimeException(result.getProblems().toString());
        }
        Expression parsedExpression = result.getResult().get();
        return compileExpression(parsedExpression);
    }

    public TranspiledExpressionResult compileExpression(Expression parsedExpression) {
        VariableAnalyser analyser = new VariableAnalyser(mvelTranspilerContext.getEvaluatorInfo().allVars().keySet());
        parsedExpression.accept(analyser, null);

        // Avoid processing the LHS as it's not present while compiling an expression
        TypedExpression compiled = new RHSPhase(mvelTranspilerContext).invoke(parsedExpression);

        Expression expression = (Expression) compiled.toJavaExpression();

        return new TranspiledExpressionResult(expression, compiled.getType(), analyser.getUsed());
    }
}
