package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;

import java.util.List;

public final class VariableParser {

    private VariableParser() {
    }

    public static ModifiersAnnotations parseVariableModifiers(List<Mvel3Parser.VariableModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.VariableModifierContext modifierContext : modifierContexts) {
                if (modifierContext.FINAL() != null) {
                    Modifier finalModifier = Modifier.finalModifier();
                    finalModifier.setTokenRange(TokenRangeConverter.createTokenRange(modifierContext));
                    modifiers.add(finalModifier);
                } else if (modifierContext.annotation() != null) {
                    annotations.add(ModifiersConverter.convertAnnotationExpr(modifierContext.annotation()));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }
}
