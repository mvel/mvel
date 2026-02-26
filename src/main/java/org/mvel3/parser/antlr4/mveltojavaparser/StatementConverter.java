package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.VarType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.WithStatement;

public final class StatementConverter {

    private StatementConverter() {
    }

    public static Node convertStatement(final Mvel3Parser.StatementContext ctx, final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle modify statement
        if (ctx.modifyStatement() != null) {
            return convertModifyStatement(ctx.modifyStatement(), mvel3toJavaParserVisitor);
        } else if (ctx.statementExpression != null) {
            // Handle expression statement: expression ';'
            Expression expr = (Expression) mvel3toJavaParserVisitor.visit(ctx.statementExpression);
            ExpressionStmt exprStmt = new ExpressionStmt(expr);
            exprStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.blockLabel != null) {
            // Handle block statement
            return BlockConverter.convertBlock(ctx.blockLabel, mvel3toJavaParserVisitor);
        } else if (ctx.IF() != null) {
            // Handle if statement: IF parExpression statement (ELSE statement)?
            Expression condition = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());
            Statement thenStmt = (Statement) convertStatement(ctx.statement(0), mvel3toJavaParserVisitor);

            IfStmt ifStmt = new IfStmt(condition, thenStmt, null);
            ifStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

            // Handle else clause if present
            if (ctx.ELSE() != null && ctx.statement().size() > 1) {
                Statement elseStmt = (Statement) convertStatement(ctx.statement(1), mvel3toJavaParserVisitor);
                ifStmt.setElseStmt(elseStmt);
            }

            return ifStmt;
        } else if (ctx.DO() != null) {
            // Handle do-while statement: DO statement WHILE parExpression ';'
            Statement body = (Statement) convertStatement(ctx.statement(0), mvel3toJavaParserVisitor);
            Expression condition = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());

            DoStmt doStmt = new DoStmt(body, condition);
            doStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return doStmt;
        } else if (ctx.WHILE() != null) {
            // Handle while statement: WHILE parExpression statement
            Expression condition = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());
            Statement body = (Statement) convertStatement(ctx.statement(0), mvel3toJavaParserVisitor);

            WhileStmt whileStmt = new WhileStmt(condition, body);
            whileStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return whileStmt;
        } else if (ctx.FOR() != null) {
            return ForConverter.convertForStatement(ctx, mvel3toJavaParserVisitor);
        } else if (ctx.SWITCH() != null) {
            return SwitchConverter.convertSwitchStatementExpression(ctx, mvel3toJavaParserVisitor);
        } else if (ctx.RETURN() != null) {
            // Handle return statement: RETURN expression? ';'
            ReturnStmt returnStmt;
            // Get the first expression after RETURN (if any)
            if (ctx.expression() != null && !ctx.expression().isEmpty()) {
                Expression expr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
                returnStmt = new ReturnStmt(expr);
            } else {
                returnStmt = new ReturnStmt();
            }
            returnStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return returnStmt;
        } else if (ctx.TRY() != null) {
            // Handle try statement
            // Grammar: TRY block (catchClause+ finallyBlock? | finallyBlock)
            //        | TRY resourceSpecification block catchClause* finallyBlock?
            NodeList<Expression> resources = new NodeList<>();
            if (ctx.resourceSpecification() != null) {
                resources = parseResources(ctx.resourceSpecification(), mvel3toJavaParserVisitor);
            }

            BlockStmt tryBlock = (BlockStmt) BlockConverter.convertBlock(ctx.block(), mvel3toJavaParserVisitor);

            NodeList<CatchClause> catchClauses = new NodeList<>();
            if (ctx.catchClause() != null) {
                for (Mvel3Parser.CatchClauseContext catchCtx : ctx.catchClause()) {
                    catchClauses.add(parseCatchClause(catchCtx, mvel3toJavaParserVisitor));
                }
            }

            BlockStmt finallyBlock = null;
            if (ctx.finallyBlock() != null) {
                finallyBlock = (BlockStmt) BlockConverter.convertBlock(ctx.finallyBlock().block(), mvel3toJavaParserVisitor);
            }

            TryStmt tryStmt = new TryStmt(resources, tryBlock, catchClauses, finallyBlock);
            tryStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return tryStmt;
        } else if (ctx.THROW() != null) {
            // Handle throw statement: THROW expression ';'
            Expression expr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
            ThrowStmt throwStmt = new ThrowStmt(expr);
            throwStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return throwStmt;
        } else if (ctx.BREAK() != null) {
            // Handle break statement: BREAK identifier? ';'
            BreakStmt breakStmt = new BreakStmt();
            if (ctx.identifier() != null) {
                breakStmt.setLabel(new SimpleName(ctx.identifier().getText()));
            }
            breakStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return breakStmt;
        } else if (ctx.CONTINUE() != null) {
            // Handle continue statement: CONTINUE identifier? ';'
            ContinueStmt continueStmt = new ContinueStmt();
            if (ctx.identifier() != null) {
                continueStmt.setLabel(new SimpleName(ctx.identifier().getText()));
            }
            continueStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return continueStmt;
        } else if (ctx.SYNCHRONIZED() != null) {
            // Handle synchronized block: SYNCHRONIZED parExpression block
            Expression expression = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());
            BlockStmt body = (BlockStmt) BlockConverter.convertBlock(ctx.block(), mvel3toJavaParserVisitor);
            SynchronizedStmt synchronizedStmt = new SynchronizedStmt(expression, body);
            synchronizedStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return synchronizedStmt;
        } else if (ctx.ASSERT() != null) {
            // Handle assert statement: ASSERT expression (':' expression)? ';'
            Expression check = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
            Expression message = null;
            if (ctx.expression().size() > 1) {
                message = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(1));
            }
            AssertStmt assertStmt = new AssertStmt(check, message);
            assertStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return assertStmt;
        } else if (ctx.identifierLabel != null) {
            // Handle labeled statement: identifierLabel = identifier ':' statement
            SimpleName label = new SimpleName(ctx.identifierLabel.getText());
            Statement innerStmt = (Statement) convertStatement(ctx.statement(0), mvel3toJavaParserVisitor);
            LabeledStmt labeledStmt = new LabeledStmt(label, innerStmt);
            labeledStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return labeledStmt;
        } else if (ctx.YIELD() != null) {
            // Handle yield statement: YIELD expression ';' (Java 17)
            Expression expr = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
            YieldStmt yieldStmt = new YieldStmt(expr);
            yieldStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return yieldStmt;
        } else if (ctx.switchExpression() != null) {
            // Handle switch expression used as statement: switchExpression ';'?
            SwitchExpr switchExpr = (SwitchExpr) SwitchConverter.convertSwitchExpression(ctx.switchExpression(), mvel3toJavaParserVisitor);
            ExpressionStmt exprStmt = new ExpressionStmt(switchExpr);
            exprStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.SEMI() != null) {
            // Handle empty statement: SEMI (bare ';')
            EmptyStmt emptyStmt = new EmptyStmt();
            emptyStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return emptyStmt;
        }
        // For now, fall back to default behavior
        return mvel3toJavaParserVisitor.visitChildren(ctx);
    }

    public static Node convertModifyStatement(final Mvel3Parser.ModifyStatementContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // modify ( identifier ) { statement* }
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // Create a NodeList for the statements
        NodeList<Statement> statements = new NodeList<>();

        // Process each statement in the modify block
        // Keep assignments as simple names - MVELToJavaRewriter will add the target prefix
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) convertStatement(stmtCtx, mvel3toJavaParserVisitor);
            statements.add(stmt);
        }

        // Create and return a ModifyStatement with proper TokenRange
        return new ModifyStatement(TokenRangeConverter.createTokenRange(ctx), target, statements);
    }

    public static Node convertWithStatement(
            final Mvel3Parser.WithStatementContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        NodeList<Statement> statements = new NodeList<>();
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) convertStatement(stmtCtx, mvel3toJavaParserVisitor);
            statements.add(stmt);
        }

        return new WithStatement(TokenRangeConverter.createTokenRange(ctx), target, statements);
    }

    private static CatchClause parseCatchClause(final Mvel3Parser.CatchClauseContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // catchClause: CATCH '(' variableModifier* catchType identifier ')' block
        // catchType: qualifiedName ('|' qualifiedName)*
        ModifiersAnnotations modifiersAnnotations = VariableParser.parseVariableModifiers(ctx.variableModifier());

        // Parse catch type (may be a union type like IOException | NullPointerException)
        Mvel3Parser.CatchTypeContext catchTypeCtx = ctx.catchType();
        Type catchType;
        if (catchTypeCtx.qualifiedName().size() == 1) {
            // Single type
            catchType = new ClassOrInterfaceType(null, catchTypeCtx.qualifiedName(0).getText());
        } else {
            // Union type: IOException | NullPointerException
            NodeList<ReferenceType> types = new NodeList<>();
            for (Mvel3Parser.QualifiedNameContext qn : catchTypeCtx.qualifiedName()) {
                types.add(new ClassOrInterfaceType(null, qn.getText()));
            }
            catchType = new UnionType(types);
        }

        SimpleName name = new SimpleName(ctx.identifier().getText());
        Parameter parameter = new Parameter(modifiersAnnotations.modifiers(),
                modifiersAnnotations.annotations(),
                catchType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        BlockStmt body = (BlockStmt) BlockConverter.convertBlock(ctx.block(), mvel3toJavaParserVisitor);

        CatchClause catchClause = new CatchClause(parameter, body);
        catchClause.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return catchClause;
    }

    private static NodeList<Expression> parseResources(
            final Mvel3Parser.ResourceSpecificationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        NodeList<Expression> resources = new NodeList<>();
        if (ctx.resources() != null) {
            for (Mvel3Parser.ResourceContext resourceCtx : ctx.resources().resource()) {
                if (resourceCtx.qualifiedName() != null && resourceCtx.expression() == null) {
                    // Simple name reference: try (existingResource) { ... }
                    resources.add(new NameExpr(resourceCtx.qualifiedName().getText()));
                } else {
                    // Variable declaration: try (Type var = expr) or try (var x = expr)
                    ModifiersAnnotations resourceModifiers = VariableParser.parseVariableModifiers(resourceCtx.variableModifier());
                    Type type;
                    String varName;
                    if (resourceCtx.VAR() != null) {
                        type = new VarType();
                        varName = resourceCtx.identifier().getText();
                    } else {
                        type = (Type) TypeConverter.convertClassOrInterfaceType(resourceCtx.classOrInterfaceType(), mvel3toJavaParserVisitor);
                        varName = resourceCtx.variableDeclaratorId().identifier().getText();
                    }
                    VariableDeclarator declarator = new VariableDeclarator(type, varName);
                    Expression initializer = (Expression) mvel3toJavaParserVisitor.visit(resourceCtx.expression());
                    declarator.setInitializer(initializer);
                    NodeList<VariableDeclarator> declarators = new NodeList<>();
                    declarators.add(declarator);
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr(resourceModifiers.modifiers(), resourceModifiers.annotations(), declarators);
                    varDecl.setTokenRange(TokenRangeConverter.createTokenRange(resourceCtx));
                    resources.add(varDecl);
                }
            }
        }
        return resources;
    }
}
