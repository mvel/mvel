package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import static org.mvel.util.ParseTools.subset;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateRuntime;

import java.io.Serializable;

public abstract class Node implements Serializable {
    protected String name;
    protected char[] contents;
    protected int begin;
    protected int cStart;
    protected int cEnd;
    protected int end;
    protected Node next;
    protected Node terminus;

    public Node() {
    }

    public Node(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
    }

    public Node(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
        this.next = next;
    }

    public abstract Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public char[] getContents() {
        return contents;
    }

    public void setContents(char[] contents) {
        this.contents = contents;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getCStart() {
        return cStart;
    }

    public void setCStart(int cStart) {
        this.cStart = cStart;
    }

    public int getCEnd() {
        return cEnd;
    }

    public void setCEnd(int cEnd) {
        this.cEnd = cEnd;
    }

    public abstract boolean demarcate(Node terminatingNode, char[] template);

    public Node getNext() {
        return next;
    }

    public Node setNext(Node next) {
        return this.next = next;
    }

    public Node getTerminus() {
        return terminus;
    }

    public void setTerminus(Node terminus) {
        this.terminus = terminus;
    }

    public void calculateContents(char[] template) {
        this.contents = subset(template, cStart, end - cStart);
    }

    public int getLength() {
        return this.end - this.begin;
    }
}
