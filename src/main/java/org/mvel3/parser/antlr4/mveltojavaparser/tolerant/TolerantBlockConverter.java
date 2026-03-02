package org.mvel3.parser.antlr4.mveltojavaparser.tolerant;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.StatementConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableConverter;

public final class TolerantBlockConverter {

    private TolerantBlockConverter() {
    }

    public static Node convertBlock(
            final Mvel3Parser.BlockContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
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
                Node node = convertBlockStatement(blockStatementCtx, mvel3toJavaParserVisitor);
                if (node instanceof Statement) {
                    Statement stmt = (Statement) node;

                    // Check if this is an incomplete expression statement
                    if (stmt instanceof ExpressionStmt) {
                        ExpressionStmt exprStmt = (ExpressionStmt) stmt;
                        Expression expr = exprStmt.getExpression();

                        if (expr instanceof FieldAccessExpr) {
                            FieldAccessExpr fieldAccess = (FieldAccessExpr) expr;
                            if (TolerantModeConstants.COMPLETION_FIELD.equals(fieldAccess.getName().asString())) {
                                if (chainedExpression == null) {
                                    // First incomplete expression
                                    chainedExpression = fieldAccess;
                                } else {
                                    // Merge with previous expression
                                    chainedExpression = mergeFieldAccessExpressions(chainedExpression, fieldAccess, mvel3toJavaParserVisitor);
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

    public static Node convertBlockStatement(
            final Mvel3Parser.BlockStatementContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (ctx.localVariableDeclaration() != null) {
            // Handle local variable declaration
            VariableDeclarationExpr varDecl = (VariableDeclarationExpr) VariableConverter.convertLocalVariableDeclaration(ctx.localVariableDeclaration(), mvel3toJavaParserVisitor);
            ExpressionStmt exprStmt = new ExpressionStmt(varDecl);
            exprStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.statement() != null) {
            return StatementConverter.convertStatement(ctx.statement(), mvel3toJavaParserVisitor);
        } else if (ctx.localTypeDeclaration() != null) {
            // TODO: Handle local type declarations if needed
            throw new UnsupportedOperationException("Local type declarations not yet implemented");
        }

        //===== Tolerant mode handling =====

        // handle error nodes
        if (areChildrenErrorNodes(ctx)) {
            // Build the complete field access chain for incomplete expressions
            Expression expr = buildIncompleteExpression(ctx, mvel3toJavaParserVisitor);
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

    private static Expression mergeFieldAccessExpressions(
            final Expression chainedExpression,
            final FieldAccessExpr newFieldAccess,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Extract the scope from the new field access (this should be the field name we want to chain)
        Expression scope = newFieldAccess.getScope();

        if (scope instanceof NameExpr) {
            // Replace the __COMPLETION_FIELD__ with the actual field name from the scope
            NameExpr fieldName = (NameExpr) scope;

            // Remove the completion marker from the chained expression and add the real field
            if (chainedExpression instanceof FieldAccessExpr) {
                FieldAccessExpr chainedFieldAccess = (FieldAccessExpr) chainedExpression;
                if (TolerantModeConstants.COMPLETION_FIELD.equals(chainedFieldAccess.getName().asString())) {
                    // Replace the completion marker with the actual field name
                    FieldAccessExpr newChained = new FieldAccessExpr(chainedFieldAccess.getScope(), fieldName.getName().asString());
                    chainedFieldAccess.getScope().setParentNode(newChained);

                    // Update tokenIdJPNodeMap: find the old orphaned node and replace with the new merged node
                    // TODO fix this hack for Tolerant visitor to work.
                    ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).updateTokenIdJPNodeMapForMerge(fieldName, newChained);

                    // Add the completion marker back at the end
                    FieldAccessExpr finalExpr = new FieldAccessExpr(newChained, TolerantModeConstants.COMPLETION_FIELD);
                    newChained.setParentNode(finalExpr);
                    return finalExpr;
                }
            }
        }

        return chainedExpression; // Fallback - return original if we can't merge
    }

    private static boolean areChildrenErrorNodes(Mvel3Parser.BlockStatementContext ctx) {
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

    private static Expression buildIncompleteExpression(
            final Mvel3Parser.BlockStatementContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
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
                        ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).getTokenIdJPNodeMap().put(((TerminalNodeImpl)child).getSymbol().getTokenIndex(), expr);
                    }
                } else {
                    // Subsequent identifier - create FieldAccessExpr
                    FieldAccessExpr fieldAccess = new FieldAccessExpr(expr, text);
                    expr.setParentNode(fieldAccess);
                    expr = fieldAccess;
                    if (child instanceof TerminalNodeImpl) {
                        ((Mvel3ToJavaParserVisitor) mvel3toJavaParserVisitor).getTokenIdJPNodeMap().put(((TerminalNodeImpl)child).getSymbol().getTokenIndex(), expr);
                    }
                }
                expectingIdentifier = false;
            } else if (text.equals(".")) {
                expectingIdentifier = true;
            }
        }

        // If we ended with a dot, add the completion marker
        if (expectingIdentifier && expr != null) {
            FieldAccessExpr completionField = new FieldAccessExpr(expr, TolerantModeConstants.COMPLETION_FIELD);
            expr.setParentNode(completionField);
            expr = completionField;
        }

        return expr;
    }
}
