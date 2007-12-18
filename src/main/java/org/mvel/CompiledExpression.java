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

import static org.mvel.MVELRuntime.execute;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.util.ParseTools.handleParserEgress;

import java.io.Serializable;

public class CompiledExpression implements Serializable, ExecutableStatement {
    private ASTIterator tokens;

    private Class knownEgressType;
    private Class knownIngressType;

    private boolean convertableIngressEgress;
    private boolean optimized = false;
    private boolean importInjectionRequired = false;
    private boolean returnBigDecimal = false;
    private boolean literalOnly;

    private Class<? extends AccessorOptimizer> accessorOptimizer;

    private String sourceName;

    private ParserContext parserContext;

    public CompiledExpression(ASTIterator astMap, String sourceName, Class egressType, ParserContext ctx, boolean literalOnly) {
        this.tokens = astMap;
        this.sourceName = sourceName;
        this.knownEgressType = egressType;
        this.literalOnly = literalOnly;
        setParserContext(ctx);
    }


    public CompiledExpression(ASTIterator astMap, String sourceName, boolean literalOnly) {
        this.tokens = astMap;
        this.sourceName = sourceName;
        this.literalOnly = literalOnly;
    }

    public ASTIterator getTokens() {
        return new ASTLinkedList(tokens.firstNode(), tokens.size());
    }

    public void setTokens(ASTIterator tokens) {
        this.tokens = tokens;
    }


    public Class getKnownEgressType() {
        return knownEgressType;
    }

    public void setKnownEgressType(Class knownEgressType) {
        this.knownEgressType = knownEgressType;
    }


    public Class getKnownIngressType() {
        return knownIngressType;
    }

    public void setKnownIngressType(Class knownIngressType) {
        this.knownIngressType = knownIngressType;
    }


    public boolean isConvertableIngressEgress() {
        return convertableIngressEgress;
    }

    public void setConvertableIngressEgress(boolean convertableIngressEgress) {
        this.convertableIngressEgress = convertableIngressEgress;
    }

    public void computeTypeConversionRule() {
        if (knownIngressType != null && knownEgressType != null) {
            convertableIngressEgress = knownIngressType.isAssignableFrom(knownEgressType);
        }
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (!optimized) setupOptimizers();
        return getValue(ctx, variableFactory);
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        if (!optimized) setupOptimizers();
        return handleParserEgress(execute(false, this, staticContext, factory), returnBigDecimal);
    }

    public Object getDirectValue(Object staticContext, VariableResolverFactory factory) {
        return execute(false, this, staticContext, factory);
    }

    private void setupOptimizers() {
        OptimizerFactory.setThreadAccessorOptimizer(accessorOptimizer);
        optimized = true;
    }

    public ASTIterator getTokenIterator() {
        //return new ASTArrayList(tokens);
        return tokens;
    }

    public boolean isOptimized() {
        return optimized;
    }

    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }

    public Class<? extends AccessorOptimizer> getAccessorOptimizer() {
        return accessorOptimizer;
    }

    public void setAccessorOptimizer(Class<? extends AccessorOptimizer> accessorOptimizer) {
        this.accessorOptimizer = accessorOptimizer;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean intOptimized() {
        return false;
    }

    public ParserContext getParserContext() {
        return parserContext;
    }

    public void setParserContext(ParserContext parserContext) {
        if ((this.parserContext = parserContext) != null) {
            this.importInjectionRequired =
                    parserContext.getImports() != null && parserContext.getImports().size() != 0;
        }
    }


    public boolean isReturnBigDecimal() {
        return returnBigDecimal;
    }

    public void setReturnBigDecimal(boolean returnBigDecimal) {
        this.returnBigDecimal = returnBigDecimal;
    }

    public boolean isImportInjectionRequired() {
        return importInjectionRequired;
    }

    public void setImportInjectionRequired(boolean importInjectionRequired) {
        this.importInjectionRequired = importInjectionRequired;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        return null;
    }

    public boolean isLiteralOnly() {
        return literalOnly;
    }
}
