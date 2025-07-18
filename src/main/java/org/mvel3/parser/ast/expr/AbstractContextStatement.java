package org.mvel3.parser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import static com.github.javaparser.utils.Utils.assertNotNull;

public class AbstractContextStatement<T extends AbstractContextStatement, R extends Expression> extends Statement {

    private Expression            target;
    private NodeList<Statement> expressions;

    public AbstractContextStatement(TokenRange tokenRange) {
        this(tokenRange, null, new NodeList<>());
    }

    public AbstractContextStatement(TokenRange tokenRange, Expression target, NodeList<Statement> expressions) {
        super(tokenRange);
        if (target != null) {
            setTarget(target);
        }
        if (expressions != null) {
            setExpressions(expressions);
        }
    }

    public R getTarget() {
        return (R) target;
    }

    public T setTarget(Expression target) {
        assertNotNull(target);
        if (target == this.target) {
            return (T) this;
        }
        notifyPropertyChange(ObservableProperty.EXPRESSION, this.target, target);
        if (this.target != null) {
            this.target.setParentNode(null);
        }
        this.target = target;
        setAsParentNodeOf(target);
        this.target = target;
        return (T) this;
    }

    public NodeList<Statement> getExpressions() {
        return expressions;
    }

    public T setExpressions(NodeList<Statement> expressions) {
        assertNotNull(expressions);
        if (expressions == this.expressions) {
            return (T) this;
        }
        notifyPropertyChange(ObservableProperty.STATEMENTS, this.expressions, expressions);
        if (this.expressions != null) {
            this.expressions.setParentNode(null);
        }
        this.expressions = expressions;

        setAsParentNodeOf(expressions); // refuses to work, wierd bug, so doing it manually

        return (T) this;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {

    }
}
