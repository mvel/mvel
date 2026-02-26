package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ReferenceType;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.type.TypeConverter;

public final class SwitchConverter {

    private SwitchConverter() {
    }

    public static Node convertSwitchExpression(
            final Mvel3Parser.SwitchExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // switchExpression: SWITCH parExpression '{' switchLabeledRule* '}'
        Expression selector = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());

        NodeList<SwitchEntry> entries = new NodeList<>();
        if (ctx.switchLabeledRule() != null) {
            for (Mvel3Parser.SwitchLabeledRuleContext ruleCtx : ctx.switchLabeledRule()) {
                entries.add(processSwitchLabeledRule(ruleCtx, mvel3toJavaParserVisitor));
            }
        }

        SwitchExpr switchExpr = new SwitchExpr(selector, entries);
        switchExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return switchExpr;
    }

    public static Node convertSwitchStatementExpression(
            final Mvel3Parser.StatementContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // Handle switch statement: SWITCH parExpression '{' switchBlockStatementGroup* switchLabel* '}'
        Expression selector = (Expression) mvel3toJavaParserVisitor.visit(ctx.parExpression().expression());

        SwitchStmt switchStmt = new SwitchStmt();
        switchStmt.setSelector(selector);
        switchStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        NodeList<SwitchEntry> entries = new NodeList<>();

        // Process switchBlockStatementGroups
        if (ctx.switchBlockStatementGroup() != null) {
            for (Mvel3Parser.SwitchBlockStatementGroupContext groupCtx : ctx.switchBlockStatementGroup()) {
                SwitchEntry entry = processSwitchBlockStatementGroup(groupCtx, mvel3toJavaParserVisitor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        // Process standalone switchLabels (if any)
        if (ctx.switchLabel() != null) {
            for (Mvel3Parser.SwitchLabelContext labelCtx : ctx.switchLabel()) {
                SwitchEntry entry = processSwitchLabel(labelCtx, mvel3toJavaParserVisitor);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        switchStmt.setEntries(entries);
        return switchStmt;
    }

    private static SwitchEntry processSwitchLabeledRule(
            final Mvel3Parser.SwitchLabeledRuleContext ruleCtx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // switchLabeledRule: CASE (expressionList | NULL_LITERAL | guardedPattern) (ARROW | COLON) switchRuleOutcome
        //                 | DEFAULT (ARROW | COLON) switchRuleOutcome

        NodeList<Expression> labels = new NodeList<>();
        boolean isArrow = ruleCtx.ARROW() != null;

        if (ruleCtx.CASE() != null) {
            if (ruleCtx.expressionList() != null) {
                // case expr1, expr2, ... ->
                for (Mvel3Parser.ExpressionContext exprCtx : ruleCtx.expressionList().expression()) {
                    labels.add((Expression) mvel3toJavaParserVisitor.visit(exprCtx));
                }
            } else if (ruleCtx.NULL_LITERAL() != null) {
                labels.add(new NullLiteralExpr());
            } else if (ruleCtx.guardedPattern() != null) {
                labels.add(processGuardedPattern(ruleCtx.guardedPattern(), mvel3toJavaParserVisitor));
            }
        }
        // DEFAULT has empty labels

        // Determine entry type and process switchRuleOutcome
        Mvel3Parser.SwitchRuleOutcomeContext outcomeCtx = ruleCtx.switchRuleOutcome();
        NodeList<Statement> statements = new NodeList<>();
        SwitchEntry.Type entryType;

        if (outcomeCtx.block() != null) {
            entryType = isArrow ? SwitchEntry.Type.BLOCK : SwitchEntry.Type.STATEMENT_GROUP;
            BlockStmt block = (BlockStmt) BlockConverter.convertBlock(outcomeCtx.block(), mvel3toJavaParserVisitor);
            statements.addAll(block.getStatements());
        } else {
            // blockStatement*
            if (isArrow) {
                // For arrow cases with a single expression, determine the type
                if (outcomeCtx.blockStatement() != null && outcomeCtx.blockStatement().size() == 1) {
                    Node node = BlockConverter.convertBlockStatement(outcomeCtx.blockStatement(0), mvel3toJavaParserVisitor);
                    if (node instanceof Statement stmt) {
                        if (stmt instanceof ThrowStmt) {
                            entryType = SwitchEntry.Type.THROWS_STATEMENT;
                        } else if (stmt instanceof ExpressionStmt) {
                            entryType = SwitchEntry.Type.EXPRESSION;
                        } else {
                            entryType = SwitchEntry.Type.STATEMENT_GROUP;
                        }
                        statements.add(stmt);
                    } else {
                        entryType = SwitchEntry.Type.STATEMENT_GROUP;
                    }
                } else {
                    entryType = SwitchEntry.Type.STATEMENT_GROUP;
                    processStatementGroup(outcomeCtx, statements, mvel3toJavaParserVisitor);
                }
            } else {
                entryType = SwitchEntry.Type.STATEMENT_GROUP;
                processStatementGroup(outcomeCtx, statements, mvel3toJavaParserVisitor);
            }
        }

        SwitchEntry entry = new SwitchEntry(labels, entryType, statements);
        entry.setTokenRange(TokenRangeConverter.createTokenRange(ruleCtx));
        return entry;
    }

    private static void processStatementGroup(
            final Mvel3Parser.SwitchRuleOutcomeContext outcomeCtx,
            final NodeList<Statement> statements,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (outcomeCtx.blockStatement() != null) {
            for (Mvel3Parser.BlockStatementContext blockStmtCtx : outcomeCtx.blockStatement()) {
                Node node = BlockConverter.convertBlockStatement(blockStmtCtx, mvel3toJavaParserVisitor);
                if (node instanceof Statement) {
                    statements.add((Statement) node);
                }
            }
        }
    }

    private static SwitchEntry processSwitchBlockStatementGroup(
            final Mvel3Parser.SwitchBlockStatementGroupContext groupCtx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // switchBlockStatementGroup: switchLabel+ blockStatement*
        if (groupCtx.switchLabel() == null || groupCtx.switchLabel().isEmpty()) {
            return null;
        }

        // Process the first switch label
        Mvel3Parser.SwitchLabelContext firstLabel = groupCtx.switchLabel(0);
        SwitchEntry entry = processSwitchLabel(firstLabel, mvel3toJavaParserVisitor);

        if (entry != null) {
            // Process statements in this switch block
            NodeList<Statement> statements = new NodeList<>();
            if (groupCtx.blockStatement() != null) {
                for (Mvel3Parser.BlockStatementContext blockStmtCtx : groupCtx.blockStatement()) {
                    Node stmt = BlockConverter.convertBlockStatement(blockStmtCtx, mvel3toJavaParserVisitor);
                    if (stmt instanceof Statement) {
                        statements.add((Statement) stmt);
                    }
                }
            }
            entry.setStatements(statements);
        }

        return entry;
    }

    private static SwitchEntry processSwitchLabel(
            final Mvel3Parser.SwitchLabelContext labelCtx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // switchLabel: CASE (constantExpression | enumConstantName | typeType varName=IDENTIFIER) ':'
        //            | DEFAULT ':'

        if (labelCtx.CASE() != null) {
            // Case label
            NodeList<Expression> labels = new NodeList<>();

            if (labelCtx.constantExpression != null) {
                // case constantExpression:
                Expression caseExpr = (Expression) mvel3toJavaParserVisitor.visit(labelCtx.constantExpression);
                if (caseExpr != null) {
                    labels.add(caseExpr);
                }
            } else if (labelCtx.enumConstantName != null) {
                // case enumConstantName:
                String enumName = labelCtx.enumConstantName.getText();
                labels.add(new NameExpr(enumName));
            } else if (labelCtx.varName != null) {
                // case typeType varName:
                ReferenceType type = (ReferenceType) TypeConverter.convertTypeType(labelCtx.typeType(), mvel3toJavaParserVisitor);
                SimpleName name = new SimpleName(labelCtx.varName.getText());
                PatternExpr patternExpr = new PatternExpr(new NodeList<>(), type, name);
                patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
                labels.add(patternExpr);
            }

            SwitchEntry entry = new SwitchEntry(labels, SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
            return entry;

        } else if (labelCtx.DEFAULT() != null) {
            // Default label
            SwitchEntry entry = new SwitchEntry(new NodeList<>(), SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
            return entry;
        }

        return null;
    }

    private static Expression processGuardedPattern(
            final Mvel3Parser.GuardedPatternContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        // guardedPattern
        //     : '(' guardedPattern ')'                                             // alt 1
        //     | variableModifier* typeType annotation* identifier ('&&' expression)*  // alt 2
        //     | guardedPattern '&&' expression                                     // alt 3

        if (ctx.LPAREN() != null) {
            // Alt 1: parenthesized guarded pattern
            Expression inner = processGuardedPattern(ctx.guardedPattern(), mvel3toJavaParserVisitor);
            EnclosedExpr enclosed = new EnclosedExpr(inner);
            enclosed.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return enclosed;
        } else if (ctx.identifier() != null) {
            // Alt 2: type pattern with optional guards
            ReferenceType type = (ReferenceType) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);
            SimpleName name = new SimpleName(ctx.identifier().getText());

            NodeList<Modifier> modifiers = new NodeList<>();
            if (ctx.variableModifier() != null) {
                for (Mvel3Parser.VariableModifierContext modCtx : ctx.variableModifier()) {
                    if (modCtx.FINAL() != null) {
                        modifiers.add(Modifier.finalModifier());
                    }
                }
            }

            PatternExpr patternExpr = new PatternExpr(modifiers, type, name);
            patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

            Expression result = patternExpr;
            if (ctx.expression() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.expression()) {
                    Expression guard = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
                    result = new BinaryExpr(result, guard, BinaryExpr.Operator.AND);
                }
            }
            return result;
        } else {
            // Alt 3: recursive guard — guardedPattern '&&' expression
            Expression left = processGuardedPattern(ctx.guardedPattern(), mvel3toJavaParserVisitor);
            Expression right = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression(0));
            return new BinaryExpr(left, right, BinaryExpr.Operator.AND);
        }
    }
}
