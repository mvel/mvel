package org.mvel3.parser.printer;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Solver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.CompilationUnitContext;
import com.github.javaparser.utils.Pair;
import org.mvel3.MVEL;
import org.mvel3.transpiler.context.Declaration;
import org.mvel3.transpiler.context.TranspilerContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mvel3.transpiler.MVELTranspiler.handleParserResult;

public class MVELToJavaRewriter {
    public static class LanguageFeatures {
        public boolean autoWrapPrimitiveWithMethod = true;
    }

    private LanguageFeatures languageFeatures = new LanguageFeatures();

    private ResolvedType bigDecimalType;
    private ResolvedType bigIntegerType;

    private ResolvedType stringType;

    private ResolvedType mapType;

    private ResolvedType listType;

    private MethodUsage mapPut;

    private MethodUsage listSet;

    private ResolvedType contextObjectType;

    private ResolvedType rootObjectType;

    private BinaryExpr   rootBinaryExpr;

    private BinaryExpr   lastBinaryExpr;

    Map<BinaryExpr, BinaryExprTypes> nodeMap;

    private CoerceRewriter coercer;

    private OverloadRewriter overloader;

    private Set<String>    declaredVars;

    private TranspilerContext context;

    private CompilationUnitContext unitContext;

    private MethodUsage mapGetMethod;
    private MethodUsage listGetMethod;

    Expression mathContext;

    public MVELToJavaRewriter(TranspilerContext context) {
        this.context = context;
        unitContext = (CompilationUnitContext) JavaParserFactory.getContext(context.getUnit(), context.getTypeSolver());
        Solver solver = context.getFacade().getSymbolSolver();
        coercer = context.getCoercer();
        overloader = context.getOverloader();

        declaredVars = new HashSet<>();

        bigDecimalType = solver.classToResolvedType(BigDecimal.class);
        bigIntegerType = solver.classToResolvedType(BigInteger.class);
        stringType = solver.classToResolvedType(String.class);
        mapType = solver.classToResolvedType(Map.class);
        listType = solver.classToResolvedType(List.class);

        mathContext = handleParserResult(context.getParser().parseExpression("java.math.MathContext.DECIMAL128"));

        if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid()) {
            Class cls = context.getEvaluatorInfo().rootDeclaration().type().getClazz();
            try {
                rootObjectType = solver.classToResolvedType(cls);
            } catch (Exception e) {
                throw new IllegalStateException("The root '" + cls + "' object must be of a Pojo Class", e);
            }
        }

        Class cls = context.getEvaluatorInfo().variableInfo().declaration().type().getClazz();
        try {
            contextObjectType = solver.classToResolvedType(cls);
        } catch (Exception e) {
            throw new IllegalStateException("The context '" + cls + "' object must be of a Pojo Class", e);
        }

        nodeMap = new IdentityHashMap<>();

        ResolvedReferenceTypeDeclaration m = mapType.asReferenceType().getTypeDeclaration().get();
        mapGetMethod = findGetterSetter("get", "", 1, m);

        ResolvedReferenceTypeDeclaration l = listType.asReferenceType().getTypeDeclaration().get();
        listGetMethod = findGetterSetter("get", "", 1, l);
    }

    private void rewriteNode(Node node) {
        BinaryExpr binExpr = null;

        switch (node.getClass().getSimpleName())  {
            case "DrlNameExpr":
            case "NameExpr":
                NameExpr nameExpr = (NameExpr) node;
                String name = nameExpr.getNameAsString();
                if (!context.getEvaluatorInfo().rootDeclaration().type().isVoid() && context.getEvaluatorInfo().rootDeclaration().name().equals(name)) {
                    // do not rewrite at root objects.
                    break;
                }

                if (!declaredVars.contains(name)) {
                    node = rewriteNameToContextObject(nameExpr);
                }
                break;
            case "FieldAccessExpr":
                node = maybeRewriteToGetter((FieldAccessExpr) node);
                break;
            case "CastExpr":
                processInlineCastExpr((CastExpr) node);
                break;
            case "AssignExpr":
                processAssignExpr((AssignExpr) node);
                break;
            case "ArrayAccessExpr":
                rewriteArrayAccessExpr((ArrayAccessExpr) node);
                break;
            case "BinaryExpr":
                binExpr = (BinaryExpr) node;
                if (rootBinaryExpr == null) {
                    rootBinaryExpr = binExpr;
                }

                lastBinaryExpr = null;
                rewriteNode( binExpr.getLeft());
                if (lastBinaryExpr == null) {
                    processBinaryExpr(binExpr, binExpr.getLeft());
                }

                lastBinaryExpr = null;
                rewriteNode( binExpr.getRight());
                if (lastBinaryExpr == null) {
                    processBinaryExpr(binExpr, binExpr.getRight());
                }

                break;
            case "ArrayCreationExpr":
                ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) node;
                if (arrayCreationExpr.getInitializer().isPresent()) {
                    ArrayInitializerExpr initExpr = ((ArrayCreationExpr) node).getInitializer().get();
                    ResolvedType resolvedType = arrayCreationExpr.getElementType().resolve();
                    rewriteArrayInitializer(resolvedType, initExpr);
                }
                break;
            case "MethodCallExpr":
                MethodCallExpr methodCall = (MethodCallExpr) node;

                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                methodCallExpr.getArguments().stream().forEach( a -> rewriteNode(a));

                // This attempts to only rewrite methods that are not called against a variable
                if (!methodCallExpr.hasScope() && !methodCallExpr.getNameAsString().contains(".")) {
                    node = rewriteMethodToContextObject(methodCallExpr);
                    methodCall = (MethodCallExpr) node;
                }

                if (methodCall.getScope().isPresent()) {
                    rewriteNode(methodCall.getScope().get());
                    Expression scope = methodCall.getScope().get();

                    ResolvedType scopeType = scope.calculateResolvedType();
                    if (languageFeatures.autoWrapPrimitiveWithMethod && scopeType.isPrimitive()) {
                        // scope is a primitive, so need to replace with Number object wrapper for it to work.
                        MethodCallExpr wrapperMethodCall = new MethodCallExpr(new NameExpr(scopeType.asPrimitive().getBoxTypeClass().getSimpleName()),
                                                                              "valueOf");
                        methodCall.removeScope();
                        methodCall.setScope(wrapperMethodCall);

                        wrapperMethodCall.addArgument(scope);
                    }
                }

                maybeCoerceArguments(methodCall);
                break;
            case "VariableDeclarationExpr":
                VariableDeclarationExpr declrExpr = (VariableDeclarationExpr) node;
                VariableDeclarator declr = declrExpr.getVariable(0);

                declaredVars.add(declr.getNameAsString());

                if ( declr.getInitializer().isPresent()) {
                    Expression initializer = declr.getInitializer().get();
                    rewriteNode(initializer);

                    initializer = declr.getInitializer().get(); // get again, incase it was written

                    // Don't coerce 'var' types, as they will be assigned to what ever the return type is
                    Type type = declr.getType();
                    if (!(initializer instanceof TextBlockLiteralExpr) &&
                        !(type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getNameAsString().equals("var"))) {
                        // This may not resolve, there is invalid syntax or types do not match parameters.
                        ResolvedType initType = initializer.calculateResolvedType();
                        Expression result =  coercer.coerce(initType, initializer, declr.getType().resolve());
                        if (result != null) {
                            declr.setInitializer(result);
                        }
                    }
                }

                break;
            default:
                rewriteChildren(node);
                break;
        }

        if ( binExpr != null) {
            lastBinaryExpr = binExpr;
        }

        if (node == rootBinaryExpr) {
            // the whole of the tree for rootBinaryExpr has been visited, so null
            rootBinaryExpr = null;
        }

    }

    private void rewriteArrayInitializer(ResolvedType elementType, ArrayInitializerExpr initExpr) {
        for (int i = 0; i < initExpr.getValues().size(); i++) {
            Expression expr = initExpr.getValues().get(i);
            if (expr.isArrayInitializerExpr()) {
                // it's nested array
                rewriteArrayInitializer(elementType, expr.asArrayInitializerExpr());
            } else {
                ResolvedType exprType = expr.calculateResolvedType();
                if (!isAssignableBy(elementType, exprType)) {
                    Expression coerced = coercer.coerce(exprType, expr, elementType);
                    if ( coerced != null) {
                        initExpr.getValues().set(i, coerced);
                    } else {
                        throw new RuntimeException("Cannot be cast or coerced: " + expr);
                    }
                }

            }
        }
    }

    private void maybeCoerceArguments(MethodCallExpr methodCall) {
        // Get the Method declaration and it's resolved types.
        // @TODO this currently does not work for method calls without scopes, i.e. member methods or static methods (mdp)
        ResolvedType scope = context.getFacade().getType(methodCall.getScope().get());
        ResolvedType resolvedScope = methodCall.getScope().get().calculateResolvedType();

        List<ResolvedMethodDeclaration> methods = scope.asReferenceType().getAllMethods();

        List<ResolvedType> argTypes = Arrays.asList(new ResolvedType[methodCall.getArguments().size()]);
        for (int i = 0; i < methodCall.getArguments().size(); i++ ) {
            argTypes.set(i, methodCall.getArguments().get(i).calculateResolvedType());
        }

        class Holder {
            private int coercionCount;
            ResolvedMethodDeclaration methodDeclr;

            List<Expression> newArgs;

            public Holder(ResolvedMethodDeclaration methodDeclr, List<Expression> newArgs, int coercionCount) {
                this.methodDeclr   = methodDeclr;
                this.newArgs       = newArgs;
                this.coercionCount = coercionCount;
            }
        }

        List<Holder> candidates = new ArrayList<>();

        // This will record all candidate methods that require one ore more coercions.
        // It will then select the candidate with the least number of coercions
        methodLoop:
        for ( ResolvedMethodDeclaration methodDeclr : methods) {
            // Find the method with same name and number of arguments
            // Then find the first subset of those, that all arguments either match or
            // can be coerced

            boolean isVariadic = false;
            int declrParamSize = methodDeclr.getNumberOfParams();
            int callArgSize = methodCall.getArguments().size();
            if ( declrParamSize > 0) {
                if ( methodDeclr.getName().startsWith("process")) {
                    System.out.println(methodDeclr);
                }
                isVariadic = methodDeclr.getLastParam().getType().isArray();
                declrParamSize = isVariadic ? methodDeclr.getNumberOfParams()-1 : methodDeclr.getNumberOfParams();
            }

            if (((isVariadic && declrParamSize <= callArgSize )|| declrParamSize == callArgSize) &&
                methodDeclr.getName().equals(methodCall.getNameAsString())) {
                // copy the list of original args, coerced ones will replace this.
                // only when we have a full match of args, will it replace the coerced ones in the methodCall
                List<Expression> newArgs = new ArrayList<>(methodCall.getArguments());

                int coercionCount = 0;
                // check each arg, see if not assignable, see it can coerce.
                for (int i = 0; i < declrParamSize; i++) {
                    coercionCount += matchCandidateParams(methodCall, methodDeclr, resolvedScope, i,i+1, argTypes, newArgs);
                }
                if (isVariadic) {
                    // process the vararg
                    coercionCount += matchCandidateParams(methodCall, methodDeclr, resolvedScope, declrParamSize, callArgSize, argTypes, newArgs);
                }
                if (coercionCount > 0) {
                    candidates.add(new Holder(methodDeclr, newArgs, coercionCount));
                } else if (coercionCount == 0){
                    // no coercions needed, this is the selected and no other candidates are irrrelevant, so clear and break
                    candidates.clear();
                    break;
                }
            }
        }

        // There are coercion candidates, find the best one and apply it's coercions
        if ( !candidates.isEmpty()) {
            Holder bestMatch = candidates.get(0);
            for (Holder candidate : candidates) {
                if (candidate.coercionCount < bestMatch.coercionCount) {
                    bestMatch = candidate;
                }
            }

            // We have a matched method name and matched arguments, use this one.
            for (int j = 0; j < bestMatch.newArgs.size(); j++) {
                if (bestMatch.newArgs.get(j) != methodCall.getArgument(j)) {
                    // the arg was replaced via coercion, replace it in the actual methodCall
                    methodCall.setArgument(j, bestMatch.newArgs.get(j));
                }
            }
        }
    }

    private int matchCandidateParams(MethodCallExpr methodCall, ResolvedMethodDeclaration methodDeclr, ResolvedType resolvedScope, int startIndex, int endIndex, List<ResolvedType> argTypes, List<Expression> newArgs) {
        ResolvedType paramType = methodDeclr.getParam(startIndex).getType();

        if (startIndex == methodDeclr.getNumberOfParams()-1 && paramType.isArray()) {
            // if vararg then unwrap to the component type
            paramType = paramType.asArrayType().getComponentType();
        }


        if (paramType.isTypeVariable()) {
            paramType = getActualResolvedTypeForTypeParameter(paramType, resolvedScope);
        }

        int coercionCount = 0;
        for(int i = startIndex; i < endIndex; i++) {
            if (!isAssignableBy(paramType, argTypes.get(i))) {
                // else try coercion
                Expression result = coercer.coerce(argTypes.get(i), methodCall.getArguments().get(i), paramType);
                if (result == null) {
                    // cannot be assiged and also cannot be coerced, so the method does not match.
                    return -1;
                }
                coercionCount++;
                newArgs.set(i, result);
            }
        }
        return coercionCount;
    }

    private static ResolvedType getActualResolvedTypeForTypeParameter(ResolvedType paramType, ResolvedType resolvedScope) {
        if (!paramType.isTypeVariable()) {
            throw new RuntimeException("Check paramType isTypeVariable before calling this method");
        }
        ResolvedTypeVariable typeVariable = paramType.asTypeVariable();
        String typeName = typeVariable.asTypeParameter().getName();
        for ( Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair : resolvedScope.asReferenceType().getTypeParametersMap() ) {
            if (typeName.equals(pair.a.asTypeParameter().getName())) {
                paramType = pair.b;
                break;
            }
        }
        return paramType;
    }

    private Expression processInlineCastExpr(CastExpr node) {
        Expression expr;

        rewriteNode(node.getExpression());

        // This is assuming the name used is a reference type. What if it was a primitive or an array? // @TODO support other ResolvedTypes (mdp)
        ResolvedType targetType = context.getFacade().convertToUsage(node.getType());

        ResolvedType sourceType = node.getExpression().calculateResolvedType();
        if (isAssignableBy(sourceType, targetType)) { // in casting the source and target are reversed
            // have to put into an () enclosure, as this was not in the original grammar due to #....#
            EnclosedExpr enclosure = new EnclosedExpr();
            node.replace(enclosure);
            enclosure.setInner(node);
            expr = enclosure; // a normal casts suffices
        } else {
            // else try coercion
            Expression result = coercer.coerce(sourceType, node.getExpression(), targetType);
            if (result == null) {
                throw new RuntimeException("Cannot be cast or coerced: " + node);
            }
            expr = result;

            node.replace(expr);
        }

        return expr;
    }

    public boolean isPublicField(ResolvedReferenceTypeDeclaration d, String name) {
        for (ResolvedFieldDeclaration f : d.getAllFields()) {
            if (f.accessSpecifier() == AccessSpecifier.PUBLIC && f.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private Expression rewriteNameToContextObject(NameExpr nameExpr) {
        String name = nameExpr.getNameAsString();
        Expression expr = nameExpr;
        if (rootObjectType != null) {
            NameExpr scope = new NameExpr(context.getEvaluatorInfo().rootDeclaration().name());

            ResolvedReferenceTypeDeclaration d = rootObjectType.asReferenceType().getTypeDeclaration().get();
            FieldAccessExpr fieldAccessExpr = new FieldAccessExpr(scope, name);

            if (isPublicField(d, name)) {
                // public field exists, so use that.
                nameExpr.replace(fieldAccessExpr);
                expr = fieldAccessExpr;
                //rewriteNode(expr);
            } else {
                // temporary swap, or type and thus method resolving will not work
                nameExpr.replace(fieldAccessExpr);

                Node result = maybeRewriteToGetter(fieldAccessExpr);
                if (result != null) {
                    expr = (Expression) result;
                } else {
                    // no getter either, so revert
                    fieldAccessExpr.replace(nameExpr);
                }
            }
        }

        if (expr != nameExpr) { // reprocess if it was rewritten
            rewriteNode(expr);
        }

        return expr;
    }

    private Expression rewriteMethodToContextObject(MethodCallExpr methodCallExpr) {
        Expression expr = methodCallExpr;
        if (rootObjectType != null) {
            // clone this, so we can try and resolve it against the root pattern as scope.
            // Note if this is a static import, the root will take priority.
            MethodCallExpr cloned = methodCallExpr.clone();
            cloned.setScope(new NameExpr(context.getEvaluatorInfo().rootDeclaration().name()));

            methodCallExpr.replace(cloned); // temporary swap, so type resolving works
            MethodUsage methodUsage = null;
            try {
                methodUsage = context.getFacade().solveMethodAsUsage(cloned);
            } catch (RuntimeException e) {
                // swallow, we check null anyway. I's dumb this is a runtime exception, as no solving is valid.
            }

            if (methodUsage != null) {
                expr = cloned;
            } else {
                // swap back again.
                cloned.replace(methodCallExpr);
            }
        }

        return expr;
    }

    private void processAssignExpr(AssignExpr node) {
        AssignExpr assignExpr = node;
        Expression target = assignExpr.getTarget();

        rewriteNode(assignExpr.getValue());

        if (target instanceof ArrayAccessExpr) {
            ArrayAccessExpr arrayAccessor = (ArrayAccessExpr) target;
            rewriteNode(arrayAccessor.getName());

            ResolvedType array = arrayAccessor.getName().calculateResolvedType();


            boolean isMap = isAssignableBy(mapType, array);
            boolean isList = isAssignableBy(listType, array);
            MethodUsage putSet = getPutSet(isMap, isList);

            Expression value = assignExpr.getValue();
            int paramIndex = 1;

            Supplier<MethodCallExpr>  methodCallSupplier = () -> {
                if (array.isArray()) {
                    return null;
                }

                MethodUsage m;
                if (isMap) {
                    m = mapGetMethod;
                } else {
                    m = listGetMethod;
                }

                return new MethodCallExpr(m.getName(), arrayAccessor.getIndex());
            };

            rewriteAssign(putSet, methodCallSupplier,
                          value, paramIndex, arrayAccessor.getName(), assignExpr, target,
                          (v) -> new Expression[] {arrayAccessor.getIndex(), v});

        } else if (target instanceof  FieldAccessExpr) {
            FieldAccessExpr fieldAccessor = (FieldAccessExpr) target;
            rewriteNode(fieldAccessor.getScope());

            Expression value = assignExpr.getValue();

            MethodUsage setter = getMethod("set", fieldAccessor, 1);
            int paramIndex = 0;

            Supplier<MethodCallExpr>  methodCallSupplier = () -> {
                MethodUsage methodUsage = getMethod("get", fieldAccessor, 0);
                return methodUsage != null ? new MethodCallExpr(methodUsage.getName()) : null;
            };

            rewriteAssign(setter, methodCallSupplier, value, paramIndex, ((FieldAccessExpr) target).getScope(), assignExpr, target,
                          (v) -> new Expression[] {v});
        } else if (target instanceof NameExpr){
            NameExpr nameExpr = (NameExpr) target;

            // If this updates a var that exists in the context, make sure the result is assigned back there too.
            if ( context.getInputs().contains(nameExpr.getNameAsString())) {
                Declaration<?> ctxDeclr = context.getEvaluatorInfo().variableInfo().declaration();
                Class ctxClass = ctxDeclr.type().getClazz();
                if (ctxDeclr.type().getClazz().isAssignableFrom(Map.class)) {

                    if (assignExpr.getParentNode().get() instanceof ExpressionStmt) {
                        // This is its own statement, so no need to wrap the assignment and return the new value
                        // a  = 5 becomes context.put("a", a = 5);
                        MethodCallExpr putMethod = new MethodCallExpr(new NameExpr(new SimpleName(ctxDeclr.name())), "put");
                        assignExpr.replace(putMethod);
                        putMethod.setArguments(NodeList.nodeList(new StringLiteralExpr(nameExpr.getNameAsString()),
                                                                 assignExpr));
                    } else {
                        // This assigment is part of some expression, so wrap the asssignment and return the new value
                        // return a = 5 becomes return org.mvel3.MVEL.putMap(context, "a", a = 5);
                        Expression scope = handleParserResult(context.getParser().parseExpression(MVEL.class.getCanonicalName()));

                        MethodCallExpr putMethod = new MethodCallExpr( scope,"putMap");
                        assignExpr.replace(putMethod);
                        putMethod.addArgument(new NameExpr("context"));
                        putMethod.addArgument(new StringLiteralExpr(nameExpr.getNameAsString()));
                        putMethod.addArgument(assignExpr);
                    }
                } else if (ctxClass.isAssignableFrom(List.class)) {
                    if (assignExpr.getParentNode().get() instanceof ExpressionStmt) {
                        // This is its own statement, so no need to wrap the assignment and return the new value
                        // a = 5 becomes context.set(i, a = 5);
                        MethodCallExpr setMethod = new MethodCallExpr(new NameExpr(new SimpleName(ctxDeclr.name())), "set");
                        assignExpr.replace(setMethod);
                        setMethod.setArguments(NodeList.nodeList(new IntegerLiteralExpr(context.getEvaluatorInfo().variableInfo().indexOf(nameExpr.getNameAsString())),
                                                                 assignExpr));
                    } else {
                        // This assigment is part of some expression, so wwrap the asssignment and return the new value
                        // return a = 5 becomes return org.mvel3.MVEL.setList(context, 3, a = 5);
                        Expression scope = handleParserResult(context.getParser().parseExpression(MVEL.class.getCanonicalName()));

                        MethodCallExpr setMethod = new MethodCallExpr( scope,"setList");
                        assignExpr.replace(setMethod);
                        setMethod.addArgument("context");
                        setMethod.addArgument(new IntegerLiteralExpr(context.getEvaluatorInfo().variableInfo().indexOf(nameExpr.getNameAsString())));
                        setMethod.addArgument(assignExpr);
                    }
                } else {
                    // pojo
                    // @TOOD I need to call the generated method below. But ideally only if it's part of some parent.
                    addSetterMethod(nameExpr);
                    MethodCallExpr setMethod = new MethodCallExpr( "contextSet" + nameExpr.getNameAsString());
                    assignExpr.replace(setMethod);
                    setMethod.addArgument(new NameExpr(ctxDeclr.name()));
                    setMethod.addArgument(assignExpr);
                }
            }

            Expression value = assignExpr.getValue();
            rewriteAssign(null, () -> null,value, -1, null, assignExpr, target,
                          (v) -> new Expression[] {v});
        }
    }

    private void rewriteAssign(MethodUsage setter, Supplier<MethodCallExpr> getterFunction,
                               Expression value, int paramIndex, Expression scope,
                               AssignExpr assignExpr, Expression target, Function<Expression, Expression[]> getArgs) {
        // Below uses target/value as well as left and right as var names.
        // This is because  it starts as an AssignExpression, which is target/value.
        // However, a compound operator will requiet a sub-part to be rewritten as
        // a BinaryExpression. At this point the target/value maps as target == left and value == right.
        // Once rewritten it maps right back to value again.

        ResolvedType targetType;
        if ( setter != null) {
            targetType = setter.getParamType(paramIndex);
        } else {
            targetType = target.calculateResolvedType();
        }

        // If the targetType is a generic TypeParameter, then resolve that
        if (targetType.isTypeVariable()) {
            targetType = getActualResolvedTypeForTypeParameter(targetType, scope.calculateResolvedType());
        }

        ResolvedType valueType = value.calculateResolvedType();

        // Does anything need coercing?
        Expression coerced = coercer.coerce(valueType, value, targetType);
        if (coerced != null) {
            value = coerced;
            valueType = targetType;
        }

        // Check this is either String assignment or numeric (which doesn't require operator overloading).
        if ( setter == null && (isAssignableBy(stringType, targetType) ||
             valueType.isNumericType() && targetType.isNumericType())) {

            // If the value was coerced, then set the coerced version.
            if (assignExpr.getValue() != value) {
                assignExpr.setValue(value);
            }

            // nothing else to rewrite
            return;
        }

        if (assignExpr.getOperator() != Operator.ASSIGN) {
            if (!isAssignableBy(targetType, valueType)){
                // No coercion possible, but types not assignable, so this cannot progress.
                throw new RuntimeException("Invalid statement with compount operator '" + assignExpr.getOperator() + "'. " + value + " cannot be coerced or assigned to " + target);
            }

            // map target and value to the left and right of the BinaryExpr
            Expression left = target;
            Expression right = value;

            // If there is a getter, use it.
            MethodCallExpr getterExpr = getterFunction.get();
            if (getterExpr != null) {
                getterExpr.setScope(scope.clone());
                left = getterExpr;
            }

            ResolvedType rightType = valueType;
            ResolvedType leftType = targetType;

            Expression overloaded = overloader.overload(leftType, left, right, getOperator(assignExpr));
            if (overloaded != null) {
                value = overloaded;
            } else {
                BinaryExpr binExpr = new BinaryExpr(left, right, getOperator(assignExpr));
                BinaryExprTypes binExprTypes = new BinaryExprTypes(binExpr);
                binExprTypes.setLeft(left, leftType);
                binExprTypes.setRight(right, rightType);

                value = binExpr;
            }
            assignExpr.setValue(value);
            assignExpr.setOperator(Operator.ASSIGN);
        }

        if (setter != null) {
            MethodCallExpr method = new MethodCallExpr(setter.getName(), getArgs.apply(value));
            method.setScope(scope);
            assignExpr.replace(method);
        } else if (assignExpr.getValue() != value) {
            assignExpr.setValue(value);
        }
    }

    public void addSetterMethod(NameExpr nameExpr) {
        MethodDeclaration methodDeclr = context.getClassDeclaration().addMethod( "contextSet" + nameExpr);
        methodDeclr.setStatic(true);
        methodDeclr.setPublic(true);


        ResolvedType propertyResolvedType = nameExpr.calculateResolvedType();
        Type propertyType = resolvedTypeToType(propertyResolvedType);
        Type contextType = resolvedTypeToType(contextObjectType);

        Parameter c = new Parameter(contextType, "context" );
        Parameter v = new Parameter(propertyType, "v" );
        methodDeclr.setParameters(NodeList.nodeList(c, v));

        methodDeclr.setType(propertyType.clone());

        ResolvedReferenceTypeDeclaration d = contextObjectType.asReferenceType().getTypeDeclaration().get();

        MethodUsage methodUsage = findGetterSetter("set", nameExpr.getNameAsString(), 1, d);

        MethodCallExpr setMethod = new MethodCallExpr(new NameExpr(new SimpleName("context")), methodUsage.getName());
        setMethod.addArgument(new NameExpr("v"));

        ReturnStmt returnStmt = new ReturnStmt(new NameExpr("v"));

        BlockStmt blockStmt = new BlockStmt();
        blockStmt.addStatement(setMethod);
        blockStmt.addStatement(returnStmt);
        methodDeclr.setBody(blockStmt);
    }

    private MethodUsage getPutSet(boolean isMap, boolean isList) {
        MethodUsage putSet = null;
        Set<MethodUsage> methods;

        if (isMap) {
            if (mapPut != null) {
                return mapPut;
            }
            methods = mapType.asReferenceType().getTypeDeclaration().get().getAllMethods();
            for (MethodUsage m : methods) {
                if (m.getName().equals("put")) {
                    putSet = m;
                    break;
                }
            }
            return mapPut = putSet;
        } else if (isList) {
            if (listSet != null) {
                return listSet;
            }
            methods = listType.asReferenceType().getTypeDeclaration().get().getAllMethods();
            for (MethodUsage m : methods) {
                if (m.getName().equals("set")) {
                    putSet = m;
                    break;
                }
            }
            return listSet = putSet;
        }

        return null;
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
            case "%": op = BinaryExpr.Operator.REMAINDER; break;
        }
        return op;
    }

    public void rewriteChildren(Node n) {
        List<Node> children = n.getChildNodes();
        if (children.isEmpty()) {
            return;
        }

        // The list must be cloned, because children are replaced as the tree is processed.
        for (Node child : new ArrayList<>(children)) {
            rewriteNode( child);
        }
    }

    public void processBinaryExpr(BinaryExpr binExpr, Node node) {
        BinaryExprTypes types = nodeMap.computeIfAbsent(binExpr, k -> new BinaryExprTypes(k));

        if ( node == binExpr.getLeft() ) {
            types.setLeft(binExpr.getLeft());
        } else if ( node == binExpr.getRight() ) {
            types.setRight(binExpr.getRight());
        }

        if (types.getLeftType() != null && types.getRightType() != null) {
            Node parent = binExpr.getParentNode().get();

            nodeMap.remove(binExpr);
            Expression overloaded = rewrite(types);
            if (overloaded != null) {
                binExpr.replace(overloaded);
            }

            if (binExpr != rootBinaryExpr) {
                Node current = parent;
                Node prev = binExpr;
                while (current.getClass() != BinaryExpr.class) {
                    prev = current;
                    current = current.getParentNode().get();
                }
                processBinaryExpr((BinaryExpr) current, prev);
            }
        }
    }

    public Expression rewrite(BinaryExprTypes binExprTypes) {
        Expression left = binExprTypes.left;
        ResolvedType leftType = binExprTypes.leftType;

        Expression right = binExprTypes.right;
        ResolvedType rightType = binExprTypes.rightType;

        boolean isLeftTypeString = isAssignableBy(stringType, leftType);
        boolean isRightTypeString = isAssignableBy(stringType, rightType);

        // This handles a special case for + and Strings in binary expressions
        if (binExprTypes.getBinaryExpr().getOperator() == BinaryExpr.Operator.PLUS &&
            (isLeftTypeString || isRightTypeString)) {
            return null;
        } // else do not coerce to String.

        Expression coerced; // only attempt right to left, if left is not String.
        if (!isLeftTypeString && ((coerced = coercer.coerce(rightType, right, leftType)) != null))  {
            right     = coerced;
            rightType = leftType;
            binExprTypes.setRight(right, rightType);
            binExprTypes.binaryExpr.setRight(right);
        } else {
            coerced = coercer.coerce(leftType, left, rightType);
            if (coerced != null) {
                left     = coerced;
                leftType = rightType;
                binExprTypes.setLeft(left, leftType);
                binExprTypes.binaryExpr.setLeft(left);
            }
        }

        Expression overloaded = overloader.overload(leftType, left, right, binExprTypes.binaryExpr.getOperator());
        return overloaded;
    }

    boolean isBigNumber(ResolvedType type) {
        if (isAssignableBy(bigDecimalType, type)) {
            return true;
        }

        if (isAssignableBy(bigIntegerType, type)) {
            return true;
        }

        return false;
    }

    public static boolean isAssignableBy(ResolvedType target, ResolvedType source) {
        if (target.isNumericType() && source.isNumericType()) {
            return true; // leave javac to pick up widenning issues
        }
        if (!source.isNull() && target.isAssignableBy(source)) {
            return true;
        }

        return false;
    }


    public static List<Expression> getArgumentsWithUnwrap(Expression e) {
        List<Expression> children = new ArrayList<>();
        if (!e.isEnclosedExpr()) {
            children.add(e);
        } else {
            // unwrap unneeded ()
            e.getChildNodes().stream()
              .map(Expression.class::cast)
              .forEach(children::add);
        }
        return children;
    }

    public Node maybeRewriteToGetter(FieldAccessExpr n) {
        rewriteNode(n.getScope());

        MethodCallExpr methodCallExpr = null;

        ResolvedType type;
        try {
            type = n.getScope().calculateResolvedType();
        } catch (Exception e) {
            // It cannot be known if 'n' is a package which cannot be resolved or a package.
            // This is a ugly way to simply do nothing if it doesn't resolve and it's assumed (maybe wrongly) it was a
            // package, instead of some other failure.
            return n;
        }
        Expression arg = null;
        try {
            MethodUsage getter = null;
            if (isAssignableBy(mapType, type)) {
                getter = mapGetMethod;
                arg    = new StringLiteralExpr(n.getNameAsString());
            } else {
                getter = getMethod("get", n, 0);
            }

            methodCallExpr = createGetterMethodCallExpr(n, getter, arg);
        } catch (Exception e) {
            // as per catch above, if it's a package, it will fail.
        }

        if (methodCallExpr != null) {
            rewriteNode(methodCallExpr);
        }

        return methodCallExpr;
    }

    private static MethodCallExpr createGetterMethodCallExpr(FieldAccessExpr n, MethodUsage getter, Expression arg) {
        if (getter != null) {
            MethodCallExpr methodCallExpr = new MethodCallExpr(getter.getName());
            if (arg != null) {
                methodCallExpr.addArgument(arg);
            }
            methodCallExpr.setScope(n.getScope());
            n.replace(methodCallExpr);

            return methodCallExpr;
        }
        return null;
    }

    public Node rewriteArrayAccessExpr(ArrayAccessExpr n) {
        if (n.getParentNode().get() instanceof  AssignExpr && ((AssignExpr)n.getParentNode().get()).getTarget() == n) {
            // do not rewrite the setter part of the ArrayAccessExpr, but getting is fine.
            return n;
        }

        rewriteNode(n.getName());

        ResolvedType resolvedType = n.getName().calculateResolvedType();

        if (resolvedType.isArray()) {
            return null;
        }

        MethodCallExpr methodCallExpr = new MethodCallExpr("get", n.getIndex());
        methodCallExpr.setScope(n.getName());

        n.replace(methodCallExpr);
        return methodCallExpr;
    }

    public MethodUsage getMethod(String getterSetter, FieldAccessExpr n, int x) {
        ResolvedReferenceTypeDeclaration d;

        try {
            ResolvedType type = n.getScope().calculateResolvedType();
            if ( languageFeatures.autoWrapPrimitiveWithMethod && type.isPrimitive()) {
                type = context.getFacade().getSymbolSolver().classToResolvedType(type.asPrimitive().getBoxTypeClass());
            }

            d = type.asReferenceType().getTypeDeclaration().get();
        } catch(Exception e) {
            // scope not resolvable, most likely a package
            return null;
        }

        try {
            if (d.getField(n.getName().asString()).accessSpecifier() == AccessSpecifier.PUBLIC) {
                // do not rewrite if it allows Public access.
                return null;
            }
        } catch (UnsolvedSymbolException e) {
           // swallow
        }

        MethodUsage candidate = findGetterSetter(getterSetter, n.getNameAsString(), x, d);
        if (candidate != null) {
            return candidate;
        }

        throw new UnsolvedSymbolException("The node has neither a public field or a getter method for : " + n);
    }

    public  static MethodUsage getMethod(String getterSetter, ResolvedType type, String name, int x) {
        ResolvedReferenceTypeDeclaration d;

        try {
            d = type.asReferenceType().getTypeDeclaration().get();
        } catch(Exception e) {
            // scope not resolvable, most likely a package
            return null;
        }

        MethodUsage candidate = findGetterSetter(getterSetter, name, x, d);
        if (candidate != null) {
            return candidate;
        }

        throw new UnsolvedSymbolException("The node has neither a public field or a getter method for : " + name);
    }

    public static MethodUsage findGetterSetter(String getterSetter, String name, int x, ResolvedReferenceTypeDeclaration d) {
        String is = null;
        if (getterSetter.equals("get")) {
            is = getterSetter("is", name);
        }
        String getterTarget = getterSetter(getterSetter, name);
        for (MethodUsage candidate : d.getAllMethods()) {
            String methodName = candidate.getName();
            if (candidate.getDeclaration().accessSpecifier() == AccessSpecifier.PUBLIC &&
                !candidate.getDeclaration().isStatic() &&
                candidate.getNoParams() == x &&
                (methodName.equals(getterTarget) || methodName.equals(name) || methodName.equals(is))) {
                return candidate;
            }
        }
        return null;
    }

    private static MethodUsage getMethod(MethodCallExpr n, String name, int x) {
        ResolvedType type = n.calculateResolvedType();
        ResolvedReferenceTypeDeclaration d = type.asReferenceType().getTypeDeclaration().get();

        for (MethodUsage candidate : d.getAllMethods()) {
            String methodName = candidate.getName();
            if (!candidate.getDeclaration().isStatic() && candidate.getNoParams() == x && methodName.equals(name)) {
                return candidate;
            }
        }

        throw new IllegalStateException("The node has neither a public field or a getter method for : " + n);
    }

    private static String getterSetter(String getterSetter, String name) {
        if ( name == null || name.isEmpty()) {
            return getterSetter;
        } else {
            return getterSetter + name.substring(0, 1).toUpperCase() + name.substring(1);
        }
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
            setLeft(left, left.calculateResolvedType());
        }

        public void setLeft(Expression left, ResolvedType leftType) {
            this.left = left;
            this.leftType = leftType;
        }

        public Expression getRight() {
            return right;
        }

        public void setRight(Expression right) {
            setRight(right, right.calculateResolvedType());
        }

        public void setRight(Expression right, ResolvedType rightType) {
            this.right = right;
            this.rightType = rightType;
        }

        public ResolvedType getLeftType() {
            return leftType;
        }

        public ResolvedType getRightType() {
            return rightType;
        }
    }

    Type resolvedTypeToType(ResolvedType resolved) {
        if(resolved.isPrimitive()) {
            ResolvedPrimitiveType asPrimitive = resolved.asPrimitive();
            switch(asPrimitive.describe().toLowerCase()) {
                case "byte": return new PrimitiveType(Primitive.BYTE);
                case "short": return new PrimitiveType(Primitive.SHORT);
                case "char": return new PrimitiveType(Primitive.CHAR);
                case "int": return new PrimitiveType(Primitive.INT);
                case "long": return new PrimitiveType(Primitive.LONG);
                case "boolean": return new PrimitiveType(Primitive.BOOLEAN);
                case "float": return new PrimitiveType(Primitive.FLOAT);
                case "double": return new PrimitiveType(Primitive.DOUBLE);
            }
        }
        return handleParserResult(context.getParser().parseClassOrInterfaceType(resolved.describe()));
    }
}

