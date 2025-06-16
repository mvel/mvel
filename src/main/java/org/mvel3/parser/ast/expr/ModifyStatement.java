package org.mvel3.parser.ast.expr;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.mvel3.parser.ast.visitor.DrlGenericVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;

import java.util.List;

import static com.github.javaparser.utils.Utils.assertNotNull;

public class ModifyStatement extends Statement {

    private NameExpr            modifyObject;
    private NodeList<Statement> expressions;

    public ModifyStatement(TokenRange tokenRange) {
        this(tokenRange, null, new NodeList<>());
    }

    public ModifyStatement(TokenRange tokenRange, NameExpr modifyObject, NodeList<Statement> expressions) {
        super(tokenRange);
        if (modifyObject != null) {
            setModifyObject(modifyObject);
        }
        if (expressions != null) {
            setExpressions(expressions);
        }
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return ((DrlGenericVisitor<R, A>)v).visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        if (v instanceof  DrlVoidVisitor) {
            ((DrlVoidVisitor<A>) v).visit(this, arg);
        }
    }

    public NameExpr getModifyObject() {
        return modifyObject;
    }

    public ModifyStatement setModifyObject(NameExpr modifyObject) {
        assertNotNull(modifyObject);
        if (modifyObject == this.modifyObject) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.EXPRESSION, this.modifyObject, modifyObject);
        if (this.modifyObject != null) {
            this.modifyObject.setParentNode(null);
        }
        this.modifyObject = modifyObject;
        setAsParentNodeOf(modifyObject);
        this.modifyObject = modifyObject;
        return this;
    }

    public NodeList<Statement> getExpressions() {
        return expressions;
    }

    public ModifyStatement setExpressions(NodeList<Statement> expressions) {
        assertNotNull(expressions);
        if (expressions == this.expressions) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.STATEMENTS, this.expressions, expressions);
        if (this.expressions != null) {
            this.expressions.setParentNode(null);
        }
        this.expressions = expressions;

        setAsParentNodeOf(expressions); // refuses to work, wierd bug, so doing it manually

        return this;
    }
}
