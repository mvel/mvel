/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
 *
 *
 */

package org.mvel3.parser.printer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import javassist.compiler.ast.BinExpr;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;
import org.mvel3.parser.ast.visitor.DrlVoidVisitorAdapter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import static com.github.javaparser.utils.PositionUtils.sortByBeginPosition;

public class MVELToJavaVisitor2 extends DrlVoidVisitorAdapter<Void> implements DrlVoidVisitor<Void> {

    private TypeSolver typeSolver;

    private JavaParserFacade facade;

    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType egressType;

    private ResolvedType widestEgressType;

    public MVELToJavaVisitor2(TypeSolver typeSolver) {
        this.typeSolver = typeSolver;

        facade = JavaParserFacade.get(typeSolver);

        bigDecimalType = new ReferenceTypeImpl(typeSolver.solveType(BigDecimal.class.getCanonicalName().toString()));
        bigIntegerType = new ReferenceTypeImpl(typeSolver.solveType(BigInteger.class.getCanonicalName().toString()));
    }

    public static class Coercion {
        static BigDecimal s2bd(short v) {
            return BigDecimal.valueOf(v);
        }

        static BigDecimal c2bd(char v) {
            return BigDecimal.valueOf(v);
        }


        static BigDecimal i2bd(int v) {
            return BigDecimal.valueOf(v);
        }

        static BigDecimal l2bd(long v) {
            return BigDecimal.valueOf(v);
        }

        static BigDecimal f2bd(float v) {
            return BigDecimal.valueOf(v);
        }

        static BigDecimal d2bd(double v) {
            return BigDecimal.valueOf(v);
        }

        static BigDecimal str2bd(String v) {
            return new BigDecimal(v, MathContext.DECIMAL128);
        }

        static BigInteger s2bi(short v) {
            return BigInteger.valueOf(v);
        }

        static BigInteger c2bi(char v) {
            return BigInteger.valueOf(v);
        }


        static BigInteger i2bi(int v) {
            return BigInteger.valueOf(v);
        }

        static BigInteger l2bi(long v) {
            return BigInteger.valueOf(v);
        }
    }

    public static class Holder {
        static Holder instance = new Holder();
        static Holder get() {
            return instance;
        }
        public Node node;
    }

    public static class BinaryExprTypes {
        private BinaryExpr binaryExpr;
        ResolvedType leftType;
        ResolvedType rightType;

        Expression left;
        Expression right;

        public BinaryExprTypes(BinaryExpr binaryExpr) {
            this.binaryExpr = binaryExpr;
        }

        public BinaryExpr getBinaryExpr() {
            return binaryExpr;
        }

        public Expression getLeft() {
            return left;
        }

        public void setLeft(Expression left) {
            this.left = left;
            this.leftType = left.calculateResolvedType();
        }

        public Expression getRight() {
            return right;
        }

        public void setRight(Expression right) {
            this.right = right;
            this.rightType = right.calculateResolvedType();
        }

        public ResolvedType getLeftType() {
            return leftType;
        }

        public ResolvedType getRightType() {
            return rightType;
        }
    }

    public List<Node> getLeaves(final Node n) {
        List<Node> leaves = new ArrayList<>();

        getLeaves(n, leaves);
        return leaves;
    }

    public void getLeaves(final Node n, List<Node> leaves ) {
        if (n.getChildNodes().isEmpty()) {
            leaves.add(n);
        } else {
            for (Node child : n.getChildNodes()) {
                getLeaves(child, leaves);
            }
        }
    }

    public void findFirstBinExpr(Node node, List<BinaryExpr> list) {
        if (node instanceof  BinaryExpr) {
            list.add((BinaryExpr) node);
            return;
        }

        for (Node child : node.getChildNodes()) {
            findFirstBinExpr(child, list);
        }
    }

    public void findAndRewriteBinExpr(Node node) {
        List<BinaryExpr> firstBinExprs = new ArrayList<>();

        findFirstBinExpr(node, firstBinExprs);

        for (BinaryExpr child : firstBinExprs) {
            // Rewrite if scan shows non-numeric (Big Number) parts
            if (!onlyNumeric(child)) {
                List<Node> leaves = getLeaves(child);
                findAndRewriteBinExpr(leaves);
            }
        }
    }

    public boolean onlyNumeric(Node node) {
        if (node instanceof  NameExpr) {
            NameExpr expr = (NameExpr) node;
            SymbolReference<? extends ResolvedValueDeclaration> ref = facade.solve(expr);
            if (ref.isSolved()) {
                return ref.getCorrespondingDeclaration().getType().isNumericType();
            } else {
                throw new RuntimeException("Unable to resolve type for " + expr);
            }
        }  else if (node instanceof FieldAccessExpr ) {
            FieldAccessExpr expr = (FieldAccessExpr) node;
            SymbolReference<? extends ResolvedValueDeclaration> ref = facade.solve(expr);
            if (ref.isSolved()) {
                return ref.getCorrespondingDeclaration().getType().isNumericType();
            } else {
                throw new RuntimeException("Unable to resolve type for " + expr);
            }
        } else if (node instanceof MethodCallExpr ) {
            MethodUsage method = facade.solveMethodAsUsage(((MethodCallExpr)node));
            return method.returnType().isNumericType();
        } else if (node instanceof ObjectCreationExpr) {
            ObjectCreationExpr expr = (ObjectCreationExpr) node;
            return expr.getType().isBoxedType();
        } else if (node instanceof LiteralExpr) {
            LiteralExpr expr = (LiteralExpr) node;
            char startChar = expr.getClass().getSimpleName().charAt(0);
            // Terrible hack.... but works for now, as long as Class names do not change.
            // have to do the reverse due to LiteralStringValueExpr and LongLiteralExpr
            switch (startChar) {
                case 'B':
                case 'N':
                case 'S':
                case 'T':
                    return false;
            }
            return true;
        } else {
            for (Node child : node.getChildNodes()) {
                if (!onlyNumeric(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void findAndRewriteBinExpr(final List<Node> nodes) {
        findAndRewriteBinExpr(nodes, new IdentityHashMap<>());
    }

    public void findAndRewriteBinExpr(final List<Node> nodes, Map<BinaryExpr, BinaryExprTypes> nodeMap) {
        List<BinaryExprTypes> rewriteQueue = new ArrayList<>();

        // Find the BinExpr, then if it has a BigNumber on atleast one branch then rewrite it.
        nodes.stream().forEach( n -> findBinExprToRewrite(n, nodeMap, rewriteQueue));
        rewriteQueue.stream().forEach( b -> rewrite(b));

        List<Node> nextNodes = rewriteQueue.stream().map(b -> b.binaryExpr.getParentNode())
                                           .filter(Optional::isPresent)
                                           .map(Optional::get)
                                           .collect(Collectors.toList());

        rewriteQueue.clear();

        if (!nextNodes.isEmpty()) {
            findAndRewriteBinExpr(nextNodes, nodeMap);
        }
    }

    @Override
    public void visit(final IfStmt n, final Void arg) {
    }

    private void findBinExprToRewrite(Node n, Map<BinaryExpr, BinaryExprTypes> nodeMap, List<BinaryExprTypes> rewriteQueue) {
        Holder.get().node = n;
        // This nodeMap of BinaryExprTypes keeps track of the left and right iteration. Only once both the left
        // and the right meets the BinaryExpr will it be rewritten.
        n.findFirst(TreeTraversal.PARENTS, n2 -> {
            Node prev = Holder.get().node;

            if (n2 instanceof  BinaryExpr) {
                BinaryExpr binEpr = (BinaryExpr) n2;
                BinaryExprTypes types = nodeMap.computeIfAbsent(binEpr, k -> new BinaryExprTypes(k));

                if ( prev == binEpr.getLeft() ) {
                    types.setLeft(binEpr.getLeft());
                } else if ( prev == binEpr.getRight() ) {
                    types.setRight(binEpr.getRight());
                }

                if (types.getLeftType() != null && types.getRightType() != null) {
                    nodeMap.remove(binEpr);
                    rewriteQueue.add(types);
                }
                return Optional.of(n2);
            }

            Holder.get().node = n2;

            return Optional.empty();

        });
    }

    boolean isBigNumber(ResolvedType type) {
        if (bigDecimalType.isAssignableBy(type) || bigIntegerType.isAssignableBy(type)) {
            return true;
        }

        return false;
    }

    public void rewrite(BinaryExprTypes binExprTypes) {
        Expression e1 = null;
        Expression e2 = null;
        if (isBigNumber(binExprTypes.leftType)) {
            e1 = binExprTypes.left;
            e2 = binExprTypes.right;
        } else if (isBigNumber(binExprTypes.rightType)) {
            e1 = binExprTypes.right;
            e2 = binExprTypes.left;
        }

        if (e1 != null && e2 != null) {
            BinaryExpr.Operator opEnum = binExprTypes.getBinaryExpr().getOperator();
            MethodCallExpr methodCallExpr = rewriteBigNumberOperator(e1, e2, opEnum);

            binExprTypes.binaryExpr.replace(methodCallExpr);
        }
    }

    private static MethodCallExpr rewriteBigNumberOperator(Expression e1, Expression e2, BinaryExpr.Operator opEnum) {
        String op = null;

        switch (opEnum) {
            case MULTIPLY : op = "multiply"; break;
            case DIVIDE : op = "divide"; break;
            case PLUS : op = "add"; break;
            case MINUS : op = "subtract"; break;
            case EQUALS : op = "compareTo"; break;
        }

        MethodCallExpr methodCallExpr;
        if (!e2.isEnclosedExpr()) {
            methodCallExpr = new MethodCallExpr(op, e2);
        } else {
            // unwrap unneeded ()
            Expression[] children = e2.getChildNodes().stream().map(Expression.class::cast)
                                      .collect(Collectors.toList())
                                      .toArray( new Expression[0]);

            methodCallExpr = new MethodCallExpr(op, children);
        }
        methodCallExpr.setScope(e1);
        e1.setParentNode(methodCallExpr);
        return methodCallExpr;
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final Void arg) {
//        Expression init = n.getVariable(0).getInitializer().get();
//        List<Node> nodes = getLeaves(init);
//        findAndRewriteBinExpr(nodes);
    }

    @Override
    public void visit(final AssignExpr n, final Void arg) {
        Expression t = n.getTarget();
        Expression v = n.getValue();

        if (onlyNumeric(v) && onlyNumeric(t)) {
            // return as there is nothing to rewrite
            return;
        }

        if (t instanceof NameExpr) {
            if (n.getOperator() != AssignExpr.Operator.ASSIGN) {
                rewriteAssignExprBinaryExpression(n);
                MVELToJavaVisitor1 mvel1 = new MVELToJavaVisitor1(typeSolver);
                mvel1.visit((MethodCallExpr) n.getValue(), arg);
                n.setOperator(AssignExpr.Operator.ASSIGN);
            }
        } else if (t instanceof FieldAccessExpr) {
            FieldAccessExpr f = (FieldAccessExpr) t;

            MethodUsage setter = getSetter(t, typeSolver);
            if (setter != null) {
                if (n.getOperator() != AssignExpr.Operator.ASSIGN) {
                    FieldAccessExpr c = f.clone();
                    BinaryExpr binExpr = rewriteAssignExprBinaryExpression(n, v, setter, c);

                    MVELToJavaVisitor1 mvel1 = new MVELToJavaVisitor1(typeSolver);
                    mvel1.visit(c, arg);
                }
            }
        }
    }

    private BinaryExpr rewriteAssignExprBinaryExpression(AssignExpr n, Expression v, MethodUsage setter, FieldAccessExpr c) {
        BinaryExpr binExpr = new BinaryExpr(c, v.clone(), getOperator(n));

        MethodCallExpr methodCallExpr = new MethodCallExpr(setter.getName(), binExpr);
        methodCallExpr.setScope(c.getScope());
        n.replace(methodCallExpr);

        List<Node>  leaves = getLeaves(binExpr);
        findAndRewriteBinExpr(leaves);

        return binExpr;
    }

    private BinaryExpr rewriteAssignExprBinaryExpression(AssignExpr n) {
        BinaryExpr binExpr = new BinaryExpr(n.getTarget().clone(), n.getValue().clone(), getOperator(n));
        n.setValue(binExpr);

        List<Node>  leaves = getLeaves(binExpr);
        findAndRewriteBinExpr(leaves);

        return binExpr;
    }

    private static BinaryExpr.Operator getOperator(AssignExpr n) {
        String opStr = n.getOperator().asString();
        opStr = opStr.substring(0, opStr.length()-1);
        BinaryExpr.Operator op = null;
        switch(opStr) {
            case "+": op = BinaryExpr.Operator.PLUS; break;
            case "-": op = BinaryExpr.Operator.MINUS; break;
            case "*": op = BinaryExpr.Operator.MULTIPLY; break;
            case "/": op = BinaryExpr.Operator.DIVIDE; break;
        }
        return op;
    }


    private static MethodUsage getSetter(Expression e, TypeSolver typeSolver) {
        MethodUsage setter = null;
        if (e instanceof FieldAccessExpr) {
            setter = getMethod("set", ((FieldAccessExpr) e), 1);
        } else if (e instanceof NameExpr) {

        }
        return setter;
    }

    private static MethodUsage getMethod(Expression e, String methodName, int numParams) {
        MethodUsage method = null;
        ResolvedType type = e.calculateResolvedType();
        ReflectionClassDeclaration d = (ReflectionClassDeclaration) type.asReferenceType().getTypeDeclaration().get();

        for (MethodUsage candidate : d.getAllMethods()) {
            if (!candidate.getDeclaration().isStatic() &&
                candidate.getName().toLowerCase().equals(methodName) && candidate.getNoParams() == numParams) {
                method = candidate;
            }
        }
        return method;
    }

    private static MethodUsage getMethod(String name, FieldAccessExpr n, int x) {
        MethodUsage method = null;
        ResolvedType type = n.getScope().calculateResolvedType();
        ReflectionClassDeclaration d = (ReflectionClassDeclaration) type.asReferenceType().getTypeDeclaration().get();
        String target = name + n.getNameAsString().toLowerCase();
        for (MethodUsage candidate : d.getAllMethods()) {
            if (!candidate.getDeclaration().isStatic() &&
                candidate.getName().toLowerCase().equals(target) && candidate.getNoParams() == x) {
                method = candidate;
            }
        }
        return method;
    }

}
