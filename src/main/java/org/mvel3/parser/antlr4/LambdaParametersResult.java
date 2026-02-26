package org.mvel3.parser.antlr4;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;

public record LambdaParametersResult(NodeList<Parameter> parameters, boolean enclosingParameters) {
}
