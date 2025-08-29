package org.mvel3.parser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.mvel3.parser.ast.visitor.DrlGenericVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;

import java.util.List;

public class WithStatement extends AbstractContextStatement<WithStatement, Expression> {

    public WithStatement(TokenRange tokenRange, Expression withExpression, NodeList<Statement> expressions) {
        super(tokenRange, withExpression, expressions);
    }
    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return ((DrlGenericVisitor<R, A>)v).visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        if (v instanceof DrlVoidVisitor) {
            ((DrlVoidVisitor<A>) v).visit(this, arg);
        }
    }

}
