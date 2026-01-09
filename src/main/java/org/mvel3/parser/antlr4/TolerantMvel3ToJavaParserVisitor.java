/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.antlr4;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

/**
 * Tolerant visitor which continues parsing even if there is an error so that code completion and analysis can be performed.
 */
public class TolerantMvel3ToJavaParserVisitor extends Mvel3ToJavaParserVisitor {

    public static final String COMPLETION_FIELD = "__COMPLETION_FIELD__";
    
    @Override
    public Node visitBlock(Mvel3Parser.BlockContext ctx) {
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.setTokenRange(createTokenRange(ctx));
        NodeList<Statement> statements = new NodeList<>();

        if (ctx.blockStatement() != null) {

            //===== Tolerant mode handling =====

            // Check if we have multiple incomplete statements that should be merged
            // For example, `System.out.` provides 4 blockStatementCtx :
            // 1. no children -> ignore
            // 2. 'System.'
            // 3. no children -> ignore
            // 4. 'out.'
            // We want to merge 2 and 4 into `System.out.__COMPLETION_FIELD__`
            Expression chainedExpression = null;

            // TODO: review if this can be simplified. Probably we can chain statements after collecting them all
            for (Mvel3Parser.BlockStatementContext blockStatementCtx : ctx.blockStatement()) {
                Node node = visit(blockStatementCtx);
                if (node instanceof Statement) {
                    Statement stmt = (Statement) node;

                    // Check if this is an incomplete expression statement
                    if (stmt instanceof ExpressionStmt) {
                        ExpressionStmt exprStmt = (ExpressionStmt) stmt;
                        Expression expr = exprStmt.getExpression();

                        if (expr instanceof FieldAccessExpr) {
                            FieldAccessExpr fieldAccess = (FieldAccessExpr) expr;
                            if (COMPLETION_FIELD.equals(fieldAccess.getName().asString())) {
                                if (chainedExpression == null) {
                                    // First incomplete expression
                                    chainedExpression = fieldAccess;
                                } else {
                                    // Merge with previous expression
                                    chainedExpression = mergeFieldAccessExpressions(chainedExpression, fieldAccess);
                                }
                                continue; // Don't add this statement yet
                            }
                        }
                    }

                    // Add any previous chained expression if we have one
                    if (chainedExpression != null) {
                        ExpressionStmt chainedStmt = new ExpressionStmt(chainedExpression);
                        chainedExpression.setParentNode(chainedStmt);
                        statements.add(chainedStmt);
                        chainedExpression = null;
                    }

                    statements.add(stmt);
                }
            }

            // Add any remaining chained expression
            if (chainedExpression != null) {
                ExpressionStmt chainedStmt = new ExpressionStmt(chainedExpression);
                chainedExpression.setParentNode(chainedStmt);
                statements.add(chainedStmt);
            }
        }

        blockStmt.setStatements(statements);

        // Set parent relationships for all statements
        for (Statement stmt : statements) {
            stmt.setParentNode(blockStmt);
        }

        return blockStmt;
    }

    @Override
    public Node visitBlockStatement(Mvel3Parser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) {
            // Handle local variable declaration
            VariableDeclarationExpr varDecl = (VariableDeclarationExpr) visit(ctx.localVariableDeclaration());
            ExpressionStmt exprStmt = new ExpressionStmt(varDecl);
            exprStmt.setTokenRange(createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.statement() != null) {
            return visit(ctx.statement());
        } else if (ctx.localTypeDeclaration() != null) {
            // TODO: Handle local type declarations if needed
            throw new UnsupportedOperationException("Local type declarations not yet implemented");
        }

        //===== Tolerant mode handling =====

        // handle error nodes
        if (areChildrenErrorNodes(ctx)) {
            // Build the complete field access chain for incomplete expressions
            Expression expr = buildIncompleteExpression(ctx);
            if (expr != null) {
                ExpressionStmt stmt = new ExpressionStmt(expr);
                expr.setParentNode(stmt);
                return stmt;
            }
        }

        if (ctx.children == null) {
            // invalid node. Ignore for now
            return null;
        }
        throw new IllegalArgumentException("Unknown blockStatement type: " + ctx.getText());
    }

    @Override
    public Node visitMemberReferenceExpression(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) visit(ctx.expression());

        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
            fieldAccess.setTokenRange(createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.methodCall() != null) {
            // Method call: expression.methodCall()
            String methodName = ctx.methodCall().identifier().getText();
            NodeList<Expression> args = parseArguments(ctx.methodCall().arguments());

            MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
            methodCall.setArguments(args);
            methodCall.setTokenRange(createTokenRange(ctx));
            return methodCall;
        } else if (ctx.THIS() != null) {
            // expression.this
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, "this");
            fieldAccess.setTokenRange(createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.SUPER() != null && ctx.superSuffix() != null) {
            // expression.super.something
            // TODO: Implement super handling
            throw new UnsupportedOperationException("Super references not yet implemented");
        } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
            // expression.new InnerClass()
            // TODO: Implement inner class creation
            throw new UnsupportedOperationException("Inner class creation not yet implemented");
        }

        //===== Tolerant mode handling =====

        // Check if this is an incomplete member access (e.g., list#ArrayList#.)
        // In tolerant mode, we want to add a COMPLETION_FIELD marker for code completion
        if (hasTrailingDot(ctx)) {
            FieldAccessExpr completionField = new FieldAccessExpr(scope, COMPLETION_FIELD);
            scope.setParentNode(completionField);
            completionField.setTokenRange(createTokenRange(ctx));
            ParseTree dot = ctx.children.get(ctx.children.size() - 1);
            if (dot instanceof TerminalNodeImpl dotTerminalNodeImpl) {
                // store the scope for the token ID (= right before the dot)
                tokenIdJPNodeMap.put(dotTerminalNodeImpl.getSymbol().getTokenIndex() - 1, scope);
            }
            return completionField;
        }

        throw new IllegalArgumentException("Unsupported member reference: " + ctx.getText());
    }

    private boolean hasTrailingDot(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Check if the last child is a DOT token (incomplete member access)
        if (ctx.children == null || ctx.children.isEmpty()) {
            return false;
        }

        ParseTree lastChild = ctx.children.get(ctx.children.size() - 1);
        return ".".equals(lastChild.getText());
    }


    private boolean areChildrenErrorNodes(Mvel3Parser.BlockStatementContext ctx) {
        if (ctx.children == null || ctx.children.isEmpty()) {
            return false; // No children to check
        }
        for (ParseTree child : ctx.children) {
            if (child instanceof ErrorNode) {
                return true; // At least one child is an error node
            }
        }
        return false;
    }

    private Expression buildIncompleteExpression(Mvel3Parser.BlockStatementContext ctx) {
        if (ctx.children == null || ctx.children.isEmpty()) {
            return null;
        }

        // Build the expression by parsing the tokens
        Expression expr = null;
        boolean expectingIdentifier = true;

        for (ParseTree child : ctx.children) {
            String text = child.getText();

            if (expectingIdentifier && !text.equals(".")) {
                // This should be an identifier
                if (expr == null) {
                    // First identifier - create NameExpr
                    expr = new NameExpr(text);
                    if (child instanceof TerminalNodeImpl) {
                        tokenIdJPNodeMap.put(((TerminalNodeImpl)child).getSymbol().getTokenIndex(), expr);
                    }
                } else {
                    // Subsequent identifier - create FieldAccessExpr
                    FieldAccessExpr fieldAccess = new FieldAccessExpr(expr, text);
                    expr.setParentNode(fieldAccess);
                    expr = fieldAccess;
                    if (child instanceof TerminalNodeImpl) {
                        tokenIdJPNodeMap.put(((TerminalNodeImpl)child).getSymbol().getTokenIndex(), expr);
                    }
                }
                expectingIdentifier = false;
            } else if (text.equals(".")) {
                expectingIdentifier = true;
            }
        }

        // If we ended with a dot, add the completion marker
        if (expectingIdentifier && expr != null) {
            FieldAccessExpr completionField = new FieldAccessExpr(expr, COMPLETION_FIELD);
            expr.setParentNode(completionField);
            expr = completionField;
        }

        return expr;
    }

    private Expression mergeFieldAccessExpressions(Expression chainedExpression, FieldAccessExpr newFieldAccess) {
        // Extract the scope from the new field access (this should be the field name we want to chain)
        Expression scope = newFieldAccess.getScope();

        if (scope instanceof NameExpr) {
            // Replace the __COMPLETION_FIELD__ with the actual field name from the scope
            NameExpr fieldName = (NameExpr) scope;

            // Remove the completion marker from the chained expression and add the real field
            if (chainedExpression instanceof FieldAccessExpr) {
                FieldAccessExpr chainedFieldAccess = (FieldAccessExpr) chainedExpression;
                if (COMPLETION_FIELD.equals(chainedFieldAccess.getName().asString())) {
                    // Replace the completion marker with the actual field name
                    FieldAccessExpr newChained = new FieldAccessExpr(chainedFieldAccess.getScope(), fieldName.getName().asString());
                    chainedFieldAccess.getScope().setParentNode(newChained);

                    // Update tokenIdJPNodeMap: find the old orphaned node and replace with the new merged node
                    updateTokenIdJPNodeMapForMerge(fieldName, newChained);

                    // Add the completion marker back at the end
                    FieldAccessExpr finalExpr = new FieldAccessExpr(newChained, COMPLETION_FIELD);
                    newChained.setParentNode(finalExpr);
                    return finalExpr;
                }
            }
        }

        return chainedExpression; // Fallback - return original if we can't merge
    }

    private void updateTokenIdJPNodeMapForMerge(NameExpr orphanedNode, FieldAccessExpr newMergedNode) {
        // Find the token ID that was pointing to the orphaned node and update it to point to the merged node
        for (Map.Entry<Integer, Node> entry : tokenIdJPNodeMap.entrySet()) {
            if (entry.getValue() == orphanedNode) {
                entry.setValue(newMergedNode);
                break;
            }
        }
    }

}
