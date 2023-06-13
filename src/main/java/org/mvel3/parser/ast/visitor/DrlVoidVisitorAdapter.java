package org.mvel3.parser.ast.visitor;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.expr.DrlxExpression;
import org.mvel3.parser.ast.expr.FullyQualifiedInlineCastExpr;
import org.mvel3.parser.ast.expr.HalfBinaryExpr;
import org.mvel3.parser.ast.expr.HalfPointFreeExpr;
import org.mvel3.parser.ast.expr.InlineCastExpr;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpression;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpressionElement;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpression;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.NullSafeFieldAccessExpr;
import org.mvel3.parser.ast.expr.NullSafeMethodCallExpr;
import org.mvel3.parser.ast.expr.OOPathChunk;
import org.mvel3.parser.ast.expr.OOPathExpr;
import org.mvel3.parser.ast.expr.PointFreeExpr;
import org.mvel3.parser.ast.expr.RuleBody;
import org.mvel3.parser.ast.expr.RuleConsequence;
import org.mvel3.parser.ast.expr.RuleDeclaration;
import org.mvel3.parser.ast.expr.RuleJoinedPatterns;
import org.mvel3.parser.ast.expr.RulePattern;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralInfiniteChunkExpr;
import org.mvel3.parser.ast.expr.WithStatement;

public class DrlVoidVisitorAdapter<A> extends VoidVisitorAdapter<A> implements DrlVoidVisitor<A> {

    public void visit(RuleDeclaration n, A arg) {
        n.getName().accept(this, arg);
        n.getRuleBody().accept(this, arg);
    }

    public void visit(RuleBody n, A arg) {
        n.getItems().accept(this, arg);
    }

    public void visit(RulePattern n, A arg) {
        n.getType().accept(this, arg);
        n.getBind().accept(this, arg);
        n.getExpr().accept(this, arg);
        // n.getType() // has no accept method
    }

    public void visit(RuleJoinedPatterns n, A arg) {
        n.getItems().accept(this, arg);
        // n.getType() // has no accept method
    }

    public void visit(DrlxExpression n, A arg) {
        n.getExpr().accept(this, arg);
        n.getBind().accept(this, arg);
    }

    public void visit(OOPathExpr n, A arg) {
        n.getChunks().accept(this, arg);
    }

    public void visit(OOPathChunk n, A arg) {
        n.getConditions().forEach( c -> c.accept(this, arg)); // Why isn't this a NodeList?
        n.getField().accept(this, arg);
        n.getInlineCast().accept(this, arg);
    }

    public void visit(RuleConsequence n, A arg) {
        n.getStatement().accept(this, arg);
    }

    public void visit(InlineCastExpr n, A arg) {
        n.getExpression().accept(this, arg);
        // n.getType() // has no accept method
    }

    public void visit(FullyQualifiedInlineCastExpr n, A arg) {
//        n.getType().accept(this, arg);
//        n.getArguments().accept(this, arg);
//        n.getExpression().accept(this, arg);
    }

    public void visit(NullSafeFieldAccessExpr n, A arg) {
        n.getName().accept(this, arg);
        n.getScope().accept(this, arg);
        n.getTypeArguments().ifPresent( t -> t.accept(this, arg));
        //n.getMetaModel() // has no acccept method.
    }

    public void visit(NullSafeMethodCallExpr n, A arg) {
        n.getArguments().accept(this, arg);
        n.getName().accept(this, arg);
        n.getScope().ifPresent(s -> s.accept(this, arg));
        n.getTypeArguments().ifPresent( t -> t.accept(this, arg));
    }

    public void visit(PointFreeExpr n, A arg) {
        n.getLeft().accept(this, arg);

        n.getOperator().accept(this, arg);

        n.getRight().accept(this, arg);
        n.getArg1().accept(this, arg);
        n.getArg2().accept(this, arg);
        n.getArg3().accept(this, arg);
        n.getArg4().accept(this, arg);
        n.getOperator().accept(this, arg);

        n.getRight().accept(this, arg);
    }

    public void visit(TemporalLiteralExpr n, A arg) {
        n.getChunks().accept(this, arg);
    }

    public void visit(TemporalLiteralChunkExpr n, A arg) {
        // n.getValue() // has no accept method
        // n.getTimeUnit() // has no accept method
    }

    public void visit(HalfBinaryExpr n, A arg) {
        // n.getOperator() // has no accept method
        // n.getMetaModel() // has no accept method
        n.getRight().accept(this, arg);
    }

    public void visit(HalfPointFreeExpr n, A arg) {
        n.getOperator().accept(this, arg);

        n.getArg1().accept(this, arg);
        n.getArg2().accept(this, arg);
        n.getArg3().accept(this, arg);
        n.getArg4().accept(this, arg);

        n.getRight().accept(this, arg);
    }

    public void visit(BigDecimalLiteralExpr n, A arg) {
        // n.getMetaModel() // has no accept method
        // n.getValue() // has no accept method
    }

    public void visit(BigIntegerLiteralExpr n, A arg) {
        // n.getMetaModel() // has no accept method
        // n.getValue() // has no accept method
    }

    public void visit(TemporalLiteralInfiniteChunkExpr n, A arg) {
        // has no getters
    }

    public void visit(DrlNameExpr n, A arg) {
        // n.getBackReferencesCount() // has no accept method
    }

    public void visit(ModifyStatement n, A arg) {
        n.getExpressions().accept(this, arg);
        n.getModifyObject().accept(this, arg);
    }

    public void visit(MapCreationLiteralExpression n, A arg) {
        n.getExpressions().accept(this, arg);
    }

    public void visit(MapCreationLiteralExpressionKeyValuePair n, A arg) {
        n.getKey().accept(this, arg);
        n.getValue().accept(this, arg);
    }

    public void visit(ListCreationLiteralExpression n, A arg) {
        n.getExpressions().accept(this, arg);
    }

    public void visit(ListCreationLiteralExpressionElement n, A arg) {
        n.getValue().accept(this, arg);
    }

    public void visit(WithStatement n, A arg) {
        n.getExpressions().accept(this, arg);
        n.getWithObject().accept(this, arg);
    }
}
