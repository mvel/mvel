package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.mveltojavaparser.type.TypeConverter;

import java.util.List;

public final class ModifiersParser {

    private ModifiersParser() {
    }

    /**
     * Walk up the ANTLR parse tree from the given context to find and parse
     * modifiers/annotations from the parent that holds them.
     * Handles all parent context types: TypeDeclaration, ClassBodyDeclaration,
     * InterfaceBodyDeclaration, LocalTypeDeclaration, AnnotationTypeElementDeclaration.
     * Also skips intermediate wrappers like GenericMethodDeclaration/GenericConstructorDeclaration.
     */
    public static ModifiersAnnotations resolveModifiersFromParent(ParserRuleContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        if (parent == null) {
            return new ModifiersAnnotations(new NodeList<>(), new NodeList<>());
        }

        // Skip intermediate wrapper contexts (GenericMethodDeclaration, GenericConstructorDeclaration)
        if (parent instanceof Mvel3Parser.GenericMethodDeclarationContext
                || parent instanceof Mvel3Parser.GenericConstructorDeclarationContext) {
            parent = parent.getParent();
        }

        if (parent instanceof Mvel3Parser.TypeDeclarationContext typeDecl) {
            if (typeDecl.classOrInterfaceModifier() != null) {
                return ModifiersParser.parseClassOrInterfaceModifiers(typeDecl.classOrInterfaceModifier());
            }
        } else if (parent instanceof Mvel3Parser.MemberDeclarationContext memberCtx) {
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx) {
                if (bodyDeclCtx.modifier() != null) {
                    return ModifiersParser.parseModifiers(bodyDeclCtx.modifier());
                }
            }
        } else if (parent instanceof Mvel3Parser.InterfaceMemberDeclarationContext memberCtx) {
            if (memberCtx.getParent() instanceof Mvel3Parser.InterfaceBodyDeclarationContext bodyDeclCtx) {
                if (bodyDeclCtx.modifier() != null) {
                    return ModifiersParser.parseModifiers(bodyDeclCtx.modifier());
                }
            }
        } else if (parent instanceof Mvel3Parser.LocalTypeDeclarationContext localCtx) {
            if (localCtx.classOrInterfaceModifier() != null) {
                return ModifiersParser.parseClassOrInterfaceModifiers(localCtx.classOrInterfaceModifier());
            }
        } else if (parent instanceof Mvel3Parser.AnnotationTypeElementRestContext restCtx) {
            if (restCtx.getParent() instanceof Mvel3Parser.AnnotationTypeElementDeclarationContext elemDeclCtx) {
                if (elemDeclCtx.modifier() != null) {
                    return ModifiersParser.parseModifiers(elemDeclCtx.modifier());
                }
            }
        }

        return new ModifiersAnnotations(new NodeList<>(), new NodeList<>());
    }

    public static ModifiersAnnotations parseModifiers(List<Mvel3Parser.ModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.ModifierContext modCtx : modifierContexts) {
                if (modCtx.classOrInterfaceModifier() != null) {
                    ModifiersAnnotations inner = parseClassOrInterfaceModifiers(List.of(modCtx.classOrInterfaceModifier()));
                    modifiers.addAll(inner.modifiers());
                    annotations.addAll(inner.annotations());
                } else if (modCtx.NATIVE() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.NATIVE, modCtx));
                } else if (modCtx.SYNCHRONIZED() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.SYNCHRONIZED, modCtx));
                } else if (modCtx.TRANSIENT() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.TRANSIENT, modCtx));
                } else if (modCtx.VOLATILE() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.VOLATILE, modCtx));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }

    public static ModifiersAnnotations parseClassOrInterfaceModifiers(List<Mvel3Parser.ClassOrInterfaceModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.ClassOrInterfaceModifierContext modCtx : modifierContexts) {
                if (modCtx.annotation() != null) {
                    annotations.add(ModifiersConverter.convertAnnotationExpr(modCtx.annotation()));
                } else if (modCtx.PUBLIC() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.PUBLIC, modCtx));
                } else if (modCtx.PROTECTED() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.PROTECTED, modCtx));
                } else if (modCtx.PRIVATE() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.PRIVATE, modCtx));
                } else if (modCtx.STATIC() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.STATIC, modCtx));
                } else if (modCtx.ABSTRACT() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.ABSTRACT, modCtx));
                } else if (modCtx.FINAL() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.FINAL, modCtx));
                } else if (modCtx.STRICTFP() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.STRICTFP, modCtx));
                } else if (modCtx.SEALED() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.SEALED, modCtx));
                } else if (modCtx.NON_SEALED() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.NON_SEALED, modCtx));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }
}
