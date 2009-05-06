package org.mvel.templates;

import org.mvel.templates.res.Node;

public class CompiledTemplate {
    private char[] template;
    private Node root;

    public CompiledTemplate(char[] template, Node root) {
        this.template = template;
        this.root = root;
    }

    public char[] getTemplate() {
        return template;
    }

    public void setTemplate(char[] template) {
        this.template = template;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }
}
