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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.ConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
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
import org.mvel3.parser.ast.expr.TemporalChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralInfiniteChunkExpr;
import org.mvel3.parser.ast.expr.WithStatement;
import org.mvel3.parser.ast.visitor.DrlVoidVisitor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.javaparser.utils.PositionUtils.sortByBeginPosition;
import static com.github.javaparser.utils.Utils.isNullOrEmpty;

public class MVELPrintVisitor extends DefaultPrettyPrinterVisitor implements DrlVoidVisitor<Void> {

    private TypeSolver typeSolver;

    private JavaParserFacade facade;

    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType egressType;

    private ResolvedType widestEgressType;

    public MVELPrintVisitor(PrinterConfiguration prettyPrinterConfiguration, TypeSolver typeSolver) {
        super(prettyPrinterConfiguration);
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

    @Override
    public void visit( RuleDeclaration n, Void arg ) {
        printComment(n.getComment(), arg);

        for (AnnotationExpr ae : n.getAnnotations()) {
            ae.accept(this, arg);
            printer.print(" ");
        }

        printer.print("rule ");
        n.getName().accept(this, arg);
        printer.println(" {");
        n.getRuleBody().accept(this, arg);
        printer.println("}");
    }

    @Override
    public void visit( RuleBody ruleBody, Void arg ) {
    }

    @Override
    public void visit(RulePattern n, Void arg) {

    }

    @Override
    public void visit(RuleJoinedPatterns n, Void arg) {

    }

    @Override
    public void visit(OOPathChunk n, Void arg) {

    }

    @Override
    public void visit(RuleConsequence n, Void arg) {

    }

    @Override
    public void visit( InlineCastExpr inlineCastExpr, Void arg ) {
        printComment(inlineCastExpr.getComment(), arg);
        inlineCastExpr.getExpression().accept( this, arg );
        printer.print( "#" );
        inlineCastExpr.getType().accept( this, arg );
    }

    @Override
    public void visit( FullyQualifiedInlineCastExpr inlineCastExpr, Void arg ) {
//        printComment(inlineCastExpr.getComment(), arg);
//        inlineCastExpr.getExpression().accept(this, arg);
//        printer.print( "#" );
//        inlineCastExpr.getType().accept(this, arg);
//        if (inlineCastExpr.hasArguments()) {
//            printer.print( "(" );
//            inlineCastExpr.getArguments().accept( this, arg );
//            printer.print( ")" );
//        }
    }

    @Override
    public void visit( NullSafeFieldAccessExpr nullSafeFieldAccessExpr, Void arg ) {
        printComment(nullSafeFieldAccessExpr.getComment(), arg);
        nullSafeFieldAccessExpr.getScope().accept( this, arg );
        printer.print( "!." );
        nullSafeFieldAccessExpr.getName().accept( this, arg );
    }

    @Override
    public void visit(NullSafeMethodCallExpr nullSafeMethodCallExpr, Void arg) {
        printComment(nullSafeMethodCallExpr.getComment(), arg);
        Optional<Expression> scopeExpression = nullSafeMethodCallExpr.getScope();
        if (scopeExpression.isPresent()) {
            scopeExpression.get().accept( this, arg );
            printer.print("!.");
        }
        printTypeArgs(nullSafeMethodCallExpr, arg);
        nullSafeMethodCallExpr.getName().accept( this, arg );
        printArguments(nullSafeMethodCallExpr.getArguments(), arg);
    }

    @Override
    public void visit( PointFreeExpr pointFreeExpr, Void arg ) {
        printComment(pointFreeExpr.getComment(), arg);
        pointFreeExpr.getLeft().accept( this, arg );
        if(pointFreeExpr.isNegated()) {
            printer.print(" not");
        }
        printer.print(" ");
        pointFreeExpr.getOperator().accept( this, arg );
        if (pointFreeExpr.getArg1() != null) {
            printer.print("[");
            pointFreeExpr.getArg1().accept( this, arg );
            if (pointFreeExpr.getArg2() != null) {
                printer.print(",");
                pointFreeExpr.getArg2().accept( this, arg );
            }
            if (pointFreeExpr.getArg3() != null) {
                printer.print(",");
                pointFreeExpr.getArg3().accept( this, arg );
            }
            if (pointFreeExpr.getArg4() != null) {
                printer.print(",");
                pointFreeExpr.getArg4().accept( this, arg );
            }
            printer.print("]");
        }
        printer.print(" ");
        NodeList<Expression> rightExprs = pointFreeExpr.getRight();
        if (rightExprs.size() == 1) {
            rightExprs.get(0).accept( this, arg );
        } else {
            printer.print("(");
            if(rightExprs.isNonEmpty()) {
                rightExprs.get(0).accept(this, arg);
            }
            for (int i = 1; i < rightExprs.size(); i++) {
                printer.print(", ");
                rightExprs.get(i).accept( this, arg );
            }
            printer.print(")");
        }
    }

    @Override
    public void visit(TemporalLiteralExpr temporalLiteralExpr, Void arg) {
        printComment(temporalLiteralExpr.getComment(), arg);
        NodeList<TemporalChunkExpr> chunks = temporalLiteralExpr.getChunks();
        for (TemporalChunkExpr c : chunks) {
            c.accept(this, arg);
        }
    }

    @Override
    public void visit(TemporalLiteralChunkExpr temporalLiteralExpr, Void arg) {
        printComment(temporalLiteralExpr.getComment(), arg);
        printer.print("" + temporalLiteralExpr.getValue());
        switch (temporalLiteralExpr.getTimeUnit()) {
            case MILLISECONDS:
                printer.print("ms");
                break;
            case SECONDS:
                printer.print("s");
                break;
            case MINUTES:
                printer.print("m");
                break;
            case HOURS:
                printer.print("h");
                break;
            case DAYS:
                printer.print("d");
                break;
        }
    }

    @Override
    public void visit(TemporalLiteralInfiniteChunkExpr temporalLiteralInfiniteChunkExpr, Void arg) {
        printer.print("*");
    }

    @Override
    public void visit(DrlxExpression expr, Void arg) {
        if (expr.getBind() != null) {
            expr.getBind().accept( this, arg );
            printer.print( " : " );
        }
        expr.getExpr().accept(this, arg);
    }

    @Override
    public void visit(OOPathExpr oopathExpr, Void arg) {
        printComment(oopathExpr.getComment(), arg);
        NodeList<OOPathChunk> chunks = oopathExpr.getChunks();
        for (int i = 0; i <  chunks.size(); i++) {
            final OOPathChunk chunk = chunks.get(i);
            printer.print(chunk.isSingleValue() ? "." : "/");
            chunk.accept(this, arg);
            printer.print(chunk.getField().toString());

            if (chunk.getInlineCast() != null) {
                printer.print("#");
                chunk.getInlineCast().accept( this, arg );
            }

            List<DrlxExpression> condition = chunk.getConditions();
            final Iterator<DrlxExpression> iterator = condition.iterator();
            if (!condition.isEmpty()) {
                printer.print("[");
                DrlxExpression first = iterator.next();
                first.accept(this, arg);
                while(iterator.hasNext()) {
                    printer.print(",");
                    iterator.next().accept(this, arg);
                }
                printer.print("]");
            }
        }
    }

    @Override
    public void visit(HalfBinaryExpr n, Void arg) {
        printComment(n.getComment(), arg);
        printer.print(n.getOperator().asString());
        printer.print(" ");
        n.getRight().accept(this, arg);
    }

    @Override
    public void visit(HalfPointFreeExpr pointFreeExpr, Void arg) {
        printComment(pointFreeExpr.getComment(), arg);
        if(pointFreeExpr.isNegated()) {
            printer.print("not ");
        }
        pointFreeExpr.getOperator().accept( this, arg );
        if (pointFreeExpr.getArg1() != null) {
            printer.print("[");
            pointFreeExpr.getArg1().accept( this, arg );
            if (pointFreeExpr.getArg2() != null) {
                printer.print(",");
                pointFreeExpr.getArg2().accept( this, arg );
            }
            if (pointFreeExpr.getArg3() != null) {
                printer.print(",");
                pointFreeExpr.getArg3().accept( this, arg );
            }
            if (pointFreeExpr.getArg4() != null) {
                printer.print(",");
                pointFreeExpr.getArg4().accept( this, arg );
            }
            printer.print("]");
        }
        printer.print(" ");
        NodeList<Expression> rightExprs = pointFreeExpr.getRight();
        if (rightExprs.size() == 1) {
            rightExprs.get(0).accept( this, arg );
        } else {
            printer.print("(");
            rightExprs.get(0).accept( this, arg );
            for (int i = 1; i < rightExprs.size(); i++) {
                printer.print(", ");
                rightExprs.get(i).accept( this, arg );
            }
            printer.print(")");
        }
    }

    @Override
    public void visit(BigDecimalLiteralExpr bigDecimalLiteralExpr, Void arg) {
        printer.print("BigDecimal.valueOf(");
        printer.print(bigDecimalLiteralExpr.asBigDecimal().toString());
        printer.print(")");
    }

    @Override
    public void visit(BigIntegerLiteralExpr bigIntegerLiteralExpr, Void arg) {
        printer.print("BigInteger.valueOf(");
        printer.print(bigIntegerLiteralExpr.asBigInteger().toString());
        printer.print(")");
    }

    @Override
    public void visit(ModifyStatement modifyExpression, Void arg) {
        printer.print("modify (");
        modifyExpression.getModifyObject().accept(this, arg);
        printer.print(") { ");

        NodeList<Statement> expressions = modifyExpression.getExpressions();
        int i = 0;
        for ( Statement st : expressions) {
            if (st == null || !st.isExpressionStmt()) {
                continue;
            }

            if ( i++ != 0) {
                printer.print(", ");
            }
            st.accept( this, arg);
        }

        printer.print(" }");

        printer.print(";");
    }

    @Override
    public void visit(WithStatement withExpression, Void arg) {
        printer.print("with (");
        withExpression.getWithObject().accept(this, arg);
        printer.print(") { ");

        String expressionWithComma = withExpression.getExpressions()
                .stream()
                .filter(Objects::nonNull)
                .filter(Statement::isExpressionStmt)
                .map(n -> PrintUtil.printNode(n.asExpressionStmt().getExpression()))
                .collect(Collectors.joining(", "));

        printer.print(expressionWithComma);
        printer.print(" }");

        printer.print(";");
    }

    public void printComment(final Optional<Comment> comment, final Void arg) {
        comment.ifPresent(c -> c.accept(this, arg));
    }


    public void printTypeArgs(final NodeWithTypeArguments<?> nodeWithTypeArguments, final Void arg) {
        NodeList<Type> typeArguments = nodeWithTypeArguments.getTypeArguments().orElse(null);
        if (!isNullOrEmpty(typeArguments)) {
            printer.print("<");
            for (final Iterator<Type> i = typeArguments.iterator(); i.hasNext(); ) {
                final Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }


    public void printArguments(final NodeList<Expression> args, final Void arg) {
        printer.print("(");
        if (!isNullOrEmpty(args)) {
            boolean columnAlignParameters = (args.size() > 1) &&
                    configuration.get(new DefaultConfigurationOption(DefaultPrinterConfiguration.ConfigOption.COLUMN_ALIGN_PARAMETERS))
                                    .map(ConfigurationOption::asBoolean).orElse(false);
            if (columnAlignParameters) {
                printer.indentWithAlignTo(printer.getCursor().column);
            }
            for (final Iterator<Expression> i = args.iterator(); i.hasNext(); ) {
                final Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(",");
                    if (columnAlignParameters) {
                        printer.println();
                    } else {
                        printer.print(" ");
                    }
                }
            }
            if (columnAlignParameters) {
                printer.unindent();
            }
        }
        printer.print(")");
    }

    @Override
    public void visit(DrlNameExpr n, Void arg) {
        printComment(n.getComment(), arg);
        java.util.stream.IntStream.range(0, n.getBackReferencesCount()).forEach(s -> printer.print("../"));
        n.getName().accept(this, arg);
    }

    @Override
    public void visit(final CharLiteralExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);

        printString(n.getValue(), "'");
    }

    private void printString(String value, String quotes) {
//        if (isBigX()) {
////            if (quotes.equals("'")) {
////                if (targetType == BigDecimal.class) {
////                    printer.print("BigDecimal.valueOf(");
////                } else if (targetType == BigInteger.class) {
////                    printer.print("BigInteger.valueOf(");
////                }
////            } else {
////                if (targetType == BigDecimal.class) {
////                    printer.print("new BigDecimal(");
////                } else if (targetType == BigInteger.class) {
////                    printer.print("new BigInteger(");
////                }
////            }
//
//            printer.print(quotes + value + quotes);
//            printer.print(")");
//        } else {
//            printer.print(quotes + value + quotes);
//        }

        printer.print(quotes + value + quotes);
    }

//    private boolean isBigX() {
//        return targetType == BigDecimal.class || targetType == BigInteger.class;
//    }

    @Override
    public void visit(final DoubleLiteralExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);
        printLiteral(n.getValue());
    }

    @Override
    public void visit(final StringLiteralExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);

        printString(n.getValue(), "\"");
    }

    private void printLiteral(String value) {
        printer.print(value);
//        if (egressType == BigDecimal.class) {
//            printer.print("BigDecimal.valueOf(" + value+ ")");
//        } else if (targetType == BigInteger.class) {
//            printer.print("BigInteger.valueOf(" + value + ")");
//        } else {
//            printer.print(value);
//        }
    }

    @Override
    public void visit(final IntegerLiteralExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);
        printLiteral(n.getValue());
    }

    @Override
    public void visit(final LongLiteralExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);
        printLiteral(n.getValue());
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

    public List<Node> getLeafs(final Expression e) {
        List<Node> leafs = new ArrayList<>();

        e.walk( n -> {
            if (n.getChildNodes().isEmpty()) {
                leafs.add(n);
            }
        });

        return leafs;
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
        Expression condition = n.getCondition();
        List<Node> nodes = getLeafs(condition);
        findAndRewriteBinExpr(nodes);
        super.visit(n, arg);
    }

    private void findBinExprToRewrite(Node n, Map<BinaryExpr, BinaryExprTypes> nodeMap, List<BinaryExprTypes> rewriteQueue) {
        Holder.get().node = n;
        // This nodeMap of BinaryExprTypes keeps track of the left and right iteration. Only once both the left
        // and the right meets the BinaryExpr will it be rewritten.
        final Node startNode = n;
        n.findFirst(TreeTraversal.PARENTS, n2 -> {
            Node prev = Holder.get().node;

            if (n2 instanceof  BinaryExpr) {
                BinaryExpr binEpr = (BinaryExpr) n2;
                BinaryExprTypes types = nodeMap.computeIfAbsent(binEpr, k -> new BinaryExprTypes(k));

                if ( prev == binEpr.getLeft() ) {
                    types.setLeft(binEpr.getLeft());
                } else if ( prev == binEpr.getRight() ) {
                    System.out.println(startNode);
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
            String op = null;
            switch (binExprTypes.getBinaryExpr().getOperator()) {
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

            binExprTypes.binaryExpr.replace(methodCallExpr);
        }
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final Void arg) {
        System.out.println(n);
        Expression init = n.getVariable(0).getInitializer().get();
        List<Node> nodes = getLeafs(init);
        findAndRewriteBinExpr(nodes);

        //rewrite(types);

//        List<Expression> bigNodes = rewriteFromLeafs(init);
//
//        System.out.println("start");
//        if (!bigNodes.isEmpty()) {
//            // rewrite
//            Object o = null;
//            bigNodes.stream().forEach( b -> {
//
//                b.walk(TreeTraversal.PARENTS, p -> {
//                    Node previous = Holder.get().node;
//                    if ( p instanceof BinaryExpr ) {
//                        BinaryExpr binExpr = (BinaryExpr) p;
//
//                        // determine if we are walking up the left or right side
//                        if ( Holder.get().node == ((BinaryExpr) p).getLeft() ) {
//                            //previous;
//                            //binExpr.replace()
//                            switch( binExpr.getOperator() ) {
//                                case MULTIPLY:
//                                    break;
//                                case DIVIDE:
//                                    break;
//                                case MINUS:
//                                    break;
//                                case PLUS:
//                                    break;
//                            };
//
//                        } else if ( Holder.get().node == ((BinaryExpr) p).getRight() ) {
//
//                        }
//                    } else if ( p instanceof  Expression && ((Expression)p).isMethodCallExpr()) {
//                        // Path was already rewritten
//                    }
//                    Holder.get().node = p;
//                });
//                //b.findFirst(BinaryExpr.class, TreeTraversal.PARENTS));
//            });
//        }

        System.out.println("done");

        //ResolvedType resolvedType = init.calculateResolvedType();


//        ResolvedType declaredType = n.getVariable(0).getType().resolve();
//        System.out.println(declaredType);

        //n.



        //type.asPrimitive().

//        ResolvedReferenceTypeDeclaration refType = type.asReferenceType().getTypeDeclaration().get();
//        ResolvedReferenceTypeDeclaration bigDecimalType = typeSolver.solveType(BigDecimal.class.getCanonicalName().toString());

//        egressType = type.asReferenceType();
//
//        egressType.asReferenceType().as



        //type.asReferenceType().as
        //ResolvedTypeDeclaration = bigDecimalType.asClass().asType();


//        if (bigDecimalType.isAssignableBy(type)) {
//            targetType = BigDecimal.class;
//        } else if (bigDecimalType.isAssignableBy(type)) {
//            targetType = BigInteger.class;
//        }

        super.visit(n, arg);
    }


    @Override
    public void visit(final BinaryExpr n, final Void arg) {
//        Expression left = n.getLeft();
//        Expression right = n.getRight();
//
//
////        if (!left.isBinaryExpr() && left.isE) {
////
////        }
//
//        if (left.isNameExpr()) {
//            ResolvedType leftType = left.calculateResolvedType();
//            if (leftType.isNumericType()) {
//
//            }
//        } else if (left.isLiteralExpr()) {
//            ResolvedType leftType = left.calculateResolvedType();
//            if (leftType.isNumericType()) {
//
//            }
//        }
//
//
//        if ( left.isObjectCreationExpr() || left.isNameExpr()) {
//            ResolvedType leftType = left.calculateResolvedType();
//            if (leftType.equals(bigDecimalType)) {
//
//            }
//        }
//
////        if ( right.isObjectCreationExpr() || left.isNameExpr() ) {
////            ResolvedType leftType = left.calculateResolvedType();
////            if (leftType.equals(bigDecimalType)) {
////
////            }
////        }
//
//        // is leaf
//        if (left.isUnaryExpr()) {
//
//        } else if (left.isNameExpr()) {
//
//        } else if (left.isLiteralExpr()) {
//
//        }



//        left.isLit

//        boolean leftIsBig = false;
//        if (left.isNameExpr() || left.isFieldAccessExpr() || left.isMethodCallExpr() || left.isObjectCreationExpr()) {
//            ResolvedType leftType = left.calculateResolvedType();
//            if (leftType.equals(bigDecimalType) || leftType.equals(bigIntegerType)) {
//                leftIsBig = true;
//            }
//        }
//
//        boolean rightIsBig = false;
//        if (right.isNameExpr() || right.isFieldAccessExpr() || right.isMethodCallExpr() || right.isObjectCreationExpr()) {
//            ResolvedType rightType = right.calculateResolvedType();
//            if (right.equals(bigDecimalType) || right.equals(bigIntegerType)) {
//                rightIsBig = true;
//            }
//        }
//
//        ResolvedType leftType = left.calculateResolvedType();
//        ResolvedType rightType = right.calculateResolvedType();
//
//        BigDecimal b1 = new BigDecimal("1");
//        BigDecimal b2 = new BigDecimal("1");
//        long l1;
//
//        b1.multiply(b2);
//
//
//
//
//
//        if (leftType.equals(bigDecimalType) && !rightType.equals(bigDecimalType)) {
//            printer.print(left + ".multiply(" + 1 + ")");
//        } else {
//
//        }
//
//
//        ResolvedType type = n.calculateResolvedType();
//
//        System.out.println(leftType + " : " + rightType + " : " + type);

        super.visit(n, arg);
    }

    @Override
    public void visit(final ArrayAccessExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);

        n.getName().accept(this, arg);

        ResolvedType type = n.getName().calculateResolvedType();


        if (type.isArray()) {
            printer.print("[");
            n.getIndex().accept(this, arg);
            printer.print("]");
        } else {
            printer.print(".get(");
            n.getIndex().accept(this, arg);
            printer.print(")");
        }
    }

    @Override
    public void visit(MapCreationLiteralExpression n, Void arg) {
        printer.print("[");

        Iterator<Expression> expressions = n.getExpressions().iterator();
        while(expressions.hasNext()) {
            expressions.next().accept(this, arg);
            if(expressions.hasNext()) {
                printer.print(", ");
            }
        }
        printer.print("]");
    }

    @Override
    public void visit(MapCreationLiteralExpressionKeyValuePair n, Void arg) {
        n.getKey().accept(this, arg);
        printer.print(" : ");
        n.getValue().accept(this, arg);
    }

    @Override
    public void visit(ListCreationLiteralExpression n, Void arg) {
        printer.print("[");

        Iterator<Expression> expressions = n.getExpressions().iterator();
        while(expressions.hasNext()) {
            expressions.next().accept(this, arg);
            if(expressions.hasNext()) {
                printer.print(", ");
            }
        }
        printer.print("]");
    }

    @Override
    public void visit(ListCreationLiteralExpressionElement n, Void arg) {
        n.getValue().accept(this, arg);
    }

    @Override
    public void visit(final AssignExpr n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);

        Expression e = n.getTarget();

        ResolvedType type = e.calculateResolvedType();
        ResolvedReferenceTypeDeclaration bigDecimalType = typeSolver.solveType(BigDecimal.class.getCanonicalName().toString());
        boolean bigDecimal = bigDecimalType.isAssignableBy(type);
        boolean bigNumber = false;
        if (bigDecimal) {
            bigNumber = true;
        } else {
            ResolvedReferenceTypeDeclaration bigIntegerType = typeSolver.solveType(BigInteger.class.getCanonicalName().toString());
            if (bigIntegerType.isAssignableBy(type)) {
                bigNumber = true;
            }
        }

        MethodUsage setter = getSetter(e, typeSolver);

        if (setter == null) {
            printOrphanCommentsBeforeThisChildNode(n);
            printComment(n.getComment(), arg);
            n.getTarget().accept(this, arg);
            if (getOption(ConfigOption.SPACE_AROUND_OPERATORS).isPresent()) {
                printer.print(" ");
            }
            if (!bigNumber) {
                printer.print(n.getOperator().asString());
                if (getOption(ConfigOption.SPACE_AROUND_OPERATORS).isPresent()) {
                    printer.print(" ");
                }
                n.getValue().accept(this, arg);
            } else {
                printer.print(" = ");
                printBigNumberExpression(n, arg, e, bigDecimal);
            }
        } else {
            Expression e2 = ((FieldAccessExpr) e).getScope();
            printer.print( e2.toString() + "." + setter.getName() + "(");

            if (n.getOperator() != Operator.ASSIGN) {
                MethodUsage getter = getMethod("get", (FieldAccessExpr) e, 0);

                printComment(n.getComment(), arg);
                ((FieldAccessExpr) e).getScope().accept(this, arg);

                printer.print("." + getter.getName() + "()");

                String oper = n.getOperator().asString();
                oper = oper.substring(0, oper.length()-1);
                printer.print(oper);
            }

            n.getValue().accept(this, arg);
            printer.print(")");
        }
    }

    private void printBigNumberExpression(AssignExpr n, Void arg, Expression e, boolean bigDecimal) {
        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);
        n.getTarget().accept(this, arg);

        if (e instanceof NameExpr) {
            String name = null;
            switch (n.getOperator()) {
                case PLUS:
                    name = "add";
                    break;
                case MINUS:
                    name = "subtract";
                    break;
                case DIVIDE:
                    name = "divide";
                    break;
                case MULTIPLY:
                    name = "multiply";
                    break;
            }
            MethodUsage method = getMethod(e, name, 1);

            printer.print("." + method.getName() + "(");



            printOrphanCommentsBeforeThisChildNode(n);
            printComment(n.getComment(), arg);

            if (getOption(ConfigOption.SPACE_AROUND_OPERATORS).isPresent()) {
                printer.print(" ");
            }

            n.getValue().accept(this, arg);

            if (bigDecimal) {
                printer.print(", java.math.MathContext.DECIMAL128");
            }

            printer.print(")");
        }
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

    @Override
    public void visit(FieldAccessExpr n, Void arg) {
        //super.visit(n, arg);
        //logPhase("FieldAccessExpr {}", n);
//        if (n.getParentNode().get() instanceof FieldAccessExpr) {
//            throw new RuntimeException("It shouldn't get this far, the visitor method should have picked this up");
//        }

        MethodUsage getter = getMethod("get", n, 0);

        printOrphanCommentsBeforeThisChildNode(n);
        printComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        if (getter != null) {
            printer.print("." + getter.getName() + "()");
        } else {
            printer.print(".");
            n.getName().accept(this, arg);
        }
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

    private Optional<ConfigurationOption> getOption(ConfigOption cOption) {
        return configuration.get(new DefaultConfigurationOption(cOption));
    }

    protected void printOrphanCommentsBeforeThisChildNode(final Node node) {
        if (!getOption(ConfigOption.PRINT_COMMENTS).isPresent()) return;
        if (node instanceof Comment) return;

        Node parent = node.getParentNode().orElse(null);
        if (parent == null) return;
        List<Node> everything = new ArrayList<>(parent.getChildNodes());
        sortByBeginPosition(everything);
        int positionOfTheChild = -1;
        for (int i = 0; i < everything.size(); ++i) { // indexOf is by equality, so this is used to index by identity
            if (everything.get(i) == node) {
                positionOfTheChild = i;
                break;
            }
        }
        if (positionOfTheChild == -1) {
            throw new AssertionError("I am not a child of my parent.");
        }
        int positionOfPreviousChild = -1;
        for (int i = positionOfTheChild - 1; i >= 0 && positionOfPreviousChild == -1; i--) {
            if (!(everything.get(i) instanceof Comment)) positionOfPreviousChild = i;
        }
        for (int i = positionOfPreviousChild + 1; i < positionOfTheChild; i++) {
            Node nodeToPrint = everything.get(i);
            if (!(nodeToPrint instanceof Comment))
                throw new RuntimeException(
                        "Expected comment, instead " + nodeToPrint.getClass() + ". Position of previous child: "
                        + positionOfPreviousChild + ", position of child " + positionOfTheChild);
            nodeToPrint.accept(this, null);
        }
    }

}
