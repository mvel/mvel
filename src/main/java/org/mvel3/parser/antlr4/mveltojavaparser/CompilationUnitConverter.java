package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

public final class CompilationUnitConverter {

    private CompilationUnitConverter() {
    }

    public static Node convertCompilationUnit(
            final Mvel3Parser.CompilationUnitContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        CompilationUnit cu = new CompilationUnit();

        if (ctx.packageDeclaration() != null) {
            Name pkg = StaticJavaParser.parseName(ctx.packageDeclaration().qualifiedName().getText());
            cu.setPackageDeclaration(new PackageDeclaration(pkg));
        }

        if (ctx.importDeclaration() != null) {
            for (Mvel3Parser.ImportDeclarationContext importDecl : ctx.importDeclaration()) {
                cu.addImport((ImportDeclaration) ImportConverter.convertImportDeclaration(importDecl));
            }
        }

        // Handle type declarations
        if (ctx.typeDeclaration() != null) {
            for (Mvel3Parser.TypeDeclarationContext typeDecl : ctx.typeDeclaration()) {
                Node node = TypeConverter.convertTypeDeclaration(typeDecl, mvel3toJavaParserVisitor);
                if (node instanceof TypeDeclaration) {
                    cu.addType((TypeDeclaration<?>) node);
                }
            }
        }

        // Handle module declaration (second alternative: moduleDeclaration EOF)
        if (ctx.moduleDeclaration() != null) {
            Mvel3Parser.ModuleDeclarationContext moduleCtx = ctx.moduleDeclaration();
            boolean isOpen = moduleCtx.OPEN() != null;
            Name moduleName = StaticJavaParser.parseName(moduleCtx.qualifiedName().getText());

            NodeList<ModuleDirective> directives = new NodeList<>();
            if (moduleCtx.moduleBody() != null && moduleCtx.moduleBody().moduleDirective() != null) {
                for (Mvel3Parser.ModuleDirectiveContext dirCtx : moduleCtx.moduleBody().moduleDirective()) {
                    directives.add(processModuleDirective(dirCtx));
                }
            }

            ModuleDeclaration moduleDecl = new ModuleDeclaration(moduleName, isOpen);
            moduleDecl.setDirectives(directives);
            cu.setModule(moduleDecl);
        }

        return cu;
    }

    private static ModuleDirective processModuleDirective(final Mvel3Parser.ModuleDirectiveContext ctx) {
        if (ctx.REQUIRES() != null) {
            NodeList<Modifier> modifiers = new NodeList<>();
            if (ctx.requiresModifier() != null) {
                for (Mvel3Parser.RequiresModifierContext modCtx : ctx.requiresModifier()) {
                    if (modCtx.TRANSITIVE() != null) {
                        modifiers.add(new Modifier(Modifier.Keyword.TRANSITIVE));
                    } else if (modCtx.STATIC() != null) {
                        modifiers.add(new Modifier(Modifier.Keyword.STATIC));
                    }
                }
            }
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            return new ModuleRequiresDirective(modifiers, name);
        } else if (ctx.EXPORTS() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> moduleNames = new NodeList<>();
            if (ctx.TO() != null && ctx.qualifiedName().size() > 1) {
                moduleNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleExportsDirective(name, moduleNames);
        } else if (ctx.OPENS() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> moduleNames = new NodeList<>();
            if (ctx.TO() != null && ctx.qualifiedName().size() > 1) {
                moduleNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleOpensDirective(name, moduleNames);
        } else if (ctx.USES() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            return new ModuleUsesDirective(name);
        } else if (ctx.PROVIDES() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> withNames = new NodeList<>();
            if (ctx.qualifiedName().size() > 1) {
                withNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleProvidesDirective(name, withNames);
        }
        throw new IllegalArgumentException("Unknown module directive: " + ctx.getText());
    }
}
