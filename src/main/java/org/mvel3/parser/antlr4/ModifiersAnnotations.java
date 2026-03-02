package org.mvel3.parser.antlr4;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;

public record ModifiersAnnotations(NodeList<Modifier> modifiers, NodeList<AnnotationExpr> annotations) {}
