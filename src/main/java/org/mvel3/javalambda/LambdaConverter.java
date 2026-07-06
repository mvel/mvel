package org.mvel3.javalambda;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;

import java.util.List;
import java.util.Map;

public final class LambdaConverter {

    private static final Map<Class<?>, PrimitiveType.Primitive> PRIMITIVES = Map.of(
            boolean.class, PrimitiveType.Primitive.BOOLEAN,
            byte.class, PrimitiveType.Primitive.BYTE,
            short.class, PrimitiveType.Primitive.SHORT,
            int.class, PrimitiveType.Primitive.INT,
            long.class, PrimitiveType.Primitive.LONG,
            float.class, PrimitiveType.Primitive.FLOAT,
            double.class, PrimitiveType.Primitive.DOUBLE,
            char.class, PrimitiveType.Primitive.CHAR
    );

    private LambdaConverter() {}

    public static MethodDeclaration toMethodDeclaration(
            LambdaExpr lambdaExpr, String methodName, List<Class<?>> parameterTypes) {

        MethodDeclaration md = new MethodDeclaration();
        md.setModifiers(Modifier.Keyword.PUBLIC);
        md.setName(methodName);
        md.setType(new VoidType());

        NodeList<Parameter> params = new NodeList<>();
        List<Parameter> lambdaParams = lambdaExpr.getParameters();
        for (int i = 0; i < lambdaParams.size(); i++) {
            Parameter original = lambdaParams.get(i);
            Type type = toJavaParserType(parameterTypes.get(i));
            params.add(new Parameter(type, original.getNameAsString()));
        }
        md.setParameters(params);

        BlockStmt body;
        if (lambdaExpr.getBody().isBlockStmt()) {
            body = lambdaExpr.getBody().asBlockStmt().clone();
        } else {
            body = new BlockStmt();
            body.addStatement(new ReturnStmt(lambdaExpr.getExpressionBody().orElseThrow().clone()));
        }
        md.setBody(body);

        return md;
    }

    private static Type toJavaParserType(Class<?> clazz) {
        if (clazz == void.class) {
            return new VoidType();
        }
        PrimitiveType.Primitive primitive = PRIMITIVES.get(clazz);
        if (primitive != null) {
            return new PrimitiveType(primitive);
        }
        return new ClassOrInterfaceType(null, clazz.getCanonicalName());
    }
}
