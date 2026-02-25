package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import org.mvel3.parser.antlr4.Mvel3Parser;

public final class ImportConverter {

    private ImportConverter() {
    }

    public static Node convertImportDeclaration(Mvel3Parser.ImportDeclarationContext ctx) {
        String importName = ctx.qualifiedName().getText();
        boolean isStatic = ctx.STATIC() != null;
        boolean isAsterisk = ctx.getChildCount() > 3 && "*".equals(ctx.getChild(ctx.getChildCount() - 2).getText());
        return new ImportDeclaration(importName, isStatic, isAsterisk);
    }
}
