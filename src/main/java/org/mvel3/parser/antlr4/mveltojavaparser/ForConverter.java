package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class ForConverter {

    private ForConverter() {
    }

    public static Node convertForStatement(
            final Mvel3Parser.StatementContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Handle for statement: FOR '(' forControl ')' statement
        Statement body = (Statement) StatementConverter.convertStatement(ctx.statement(0), mvel3toJavaParserVisitor);

        // Check if this is an enhanced for loop (foreach)
        if (ctx.forControl() != null && ctx.forControl().enhancedForControl() != null) {
            // Create ForEachStmt for enhanced for loops
            ForEachStmt forEachStmt = new ForEachStmt();
            forEachStmt.setBody(body);
            visitEnhancedForControlAndPopulate(ctx.forControl().enhancedForControl(), forEachStmt, mvel3toJavaParserVisitor);
            forEachStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return forEachStmt;
        } else {
            // Create regular ForStmt for traditional for loops
            ForStmt forStmt = new ForStmt();
            forStmt.setBody(body);

            // Parse forControl if available
            if (ctx.forControl() != null) {
                visitForControlAndPopulate(ctx.forControl(), forStmt, mvel3toJavaParserVisitor);
            }

            forStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return forStmt;
        }
    }

    private static void visitForControlAndPopulate(
            final Mvel3Parser.ForControlContext forControlCtx,
            final ForStmt forStmt,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // FOR control can be: forInit? ';' expression? ';' forUpdate?
        // Or enhanced for: variableDeclarator ':' expression

        // Check if it's an enhanced for (for-each) loop
        if (forControlCtx.enhancedForControl() != null) {
            // Enhanced for: for (Type var : iterable)
            // TODO: Implement enhanced for loop parsing if needed
            return;
        }

        // Regular for loop: for (init; condition; update)
        NodeList<Expression> initialization = new NodeList<>();
        Expression compare = null;
        NodeList<Expression> update = new NodeList<>();

        // Parse forInit
        if (forControlCtx.forInit() != null) {
            // forInit can be localVariableDeclaration or expressionList
            if (forControlCtx.forInit().localVariableDeclaration() != null) {
                // Variable declaration like: int i = 0
                initialization.add((VariableDeclarationExpr) VariableConverter.convertLocalVariableDeclaration(forControlCtx.forInit().localVariableDeclaration(), mvel3toJavaParserVisitor));
            } else if (forControlCtx.forInit().expressionList() != null) {
                // Expression list like: i = 0, j = 1
                for (Mvel3Parser.ExpressionContext exprCtx : forControlCtx.forInit().expressionList().expression()) {
                    Expression expr = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
                    if (expr != null) {
                        initialization.add(expr);
                    }
                }
            }
        }

        // Parse condition
        if (forControlCtx.expression() != null) {
            compare = (Expression) mvel3toJavaParserVisitor.visit(forControlCtx.expression());
        }

        // Parse forUpdate
        if (forControlCtx.forUpdate != null) {
            for (Mvel3Parser.ExpressionContext exprCtx : forControlCtx.forUpdate.expression()) {
                Expression expr = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
                if (expr != null) {
                    update.add(expr);
                }
            }
        }

        // Set the for loop components - only set non-empty lists
        if (!initialization.isEmpty()) {
            forStmt.setInitialization(initialization);
        }
        if (compare != null) {
            forStmt.setCompare(compare);
        }
        if (!update.isEmpty()) {
            forStmt.setUpdate(update);
        }
    }

    private static void visitEnhancedForControlAndPopulate(
            final Mvel3Parser.EnhancedForControlContext enhancedForCtx,
            final ForEachStmt forEachStmt,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // enhancedForControl: variableModifier* (typeType | VAR) variableDeclaratorId ':' expression

        // Extract variable modifiers (final, annotations)
        ModifiersAnnotations varModifiers = VariableParser.parseVariableModifiers(enhancedForCtx.variableModifier());

        // Extract type (typeType or VAR)
        Type variableType = null;
        if (enhancedForCtx.typeType() != null) {
            variableType = (Type) TypeConverter.convertTypeType(enhancedForCtx.typeType(), mvel3toJavaParserVisitor);
        } else if (enhancedForCtx.VAR() != null) {
            // Handle var type - use VarType from JavaParser
            variableType = new VarType();
        }

        // Extract variable name from variableDeclaratorId
        String variableName = enhancedForCtx.variableDeclaratorId().identifier().getText();

        // Create VariableDeclarator
        VariableDeclarator variableDeclarator = new VariableDeclarator(variableType, variableName);

        // Create VariableDeclarationExpr with modifiers
        NodeList<VariableDeclarator> variables = new NodeList<>();
        variables.add(variableDeclarator);
        VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varModifiers.modifiers(), varModifiers.annotations(), variables);

        // Extract iterable expression
        Expression iterable = (Expression) mvel3toJavaParserVisitor.visit(enhancedForCtx.expression());

        // Set the ForEachStmt components to match mvel.jj: ForEachStmt(range, varExpr, expr, body)
        forEachStmt.setVariable(varDecl);
        forEachStmt.setIterable(iterable);
    }
}
