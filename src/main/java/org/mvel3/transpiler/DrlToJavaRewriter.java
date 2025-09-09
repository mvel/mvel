package org.mvel3.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.drools.base.RuleBase;
import org.drools.base.RuleBuildContext;
import org.drools.base.base.ObjectType;
import org.drools.base.base.ValueResolver;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.base.reteoo.BaseTuple;
import org.drools.base.rule.ContextEntry;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.base.rule.constraint.BetaNodeFieldConstraint;
import org.drools.base.rule.constraint.Constraint;
import org.drools.core.RuleBaseConfiguration;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.Tuple;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.util.bitmask.BitMask;
import org.kie.api.runtime.rule.FactHandle;
import org.mvel3.parser.ast.expr.RuleDeclaration;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DrlToJavaRewriter {
    private CompilationUnit compUnit;

    public void setupImports() {
        compUnit.addImport("import org.drools.base.definitions.rule.impl.RuleImpl;");
        compUnit.addImport("import org.drools.model.Index.ConstraintType;");

    }
    private void rewriteNode(Node node) {
        BinaryExpr binExpr = null;

        switch (node.getClass().getSimpleName())  {
            case "RuleDeclaration": {
                RuleDeclaration   ruleDeclr = (RuleDeclaration) node;
                //MethodDeclaration methodDeclr = new MethodDeclaration( NodeList.nodeList(Modifier.publicModifier()), new VoidType(), ruleDeclr.getNameAsString() );
                BlockStmt body = new BlockStmt();
//                MethodCallExpr createRule = new MethodCallExpr(new NameExpr("D"), "rule", NodeList.nodeList(new StringLiteralExpr(ruleDeclr.getNameAsString())));
//                VariableDeclarationExpr ruleBuilder = new VariableDeclarationExpr();
//                ruleBuilder.getVariable(0).setInitializer(createRule);

                //body.addStatement()
            }
        }
    }

    public void x() {
        RuleImpl ruleImpl = new RuleImpl("");
        Pattern pattern = new Pattern();
        //new AlphaNodeFieldConstraint()
        pattern.addConstraint(new MarkAlphaConstraint() {
            @Override
            public boolean isAllowed(FactHandle factHandle, ValueResolver valueResolver) {
                Person x = (Person) factHandle.getObject();
                return x.getName().equals("John");
            }
        });
        //pattern.addConstraint();
        //ruleImpl.addPattern();
    }

    public static abstract class MarkAlphaConstraint implements AlphaNodeFieldConstraint {
        public MarkAlphaConstraint() {
        }

        @Override
        public AlphaNodeFieldConstraint cloneIfInUse() {
            return null;
        }

        @Override
        public Declaration[] getRequiredDeclarations() {
            return new Declaration[0];
        }

        @Override
        public void replaceDeclaration(Declaration declaration, Declaration declaration1) {

        }

        @Override
        public Constraint clone() {
            return null;
        }

        @Override
        public ConstraintType getType() {
            return null;
        }

        @Override
        public boolean isTemporal() {
            return false;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {

        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        }

        @Override
        public BitMask getListenedPropertyMask(ObjectType objectType, List<String> settableProperties) {
            return AlphaNodeFieldConstraint.super.getListenedPropertyMask(objectType, settableProperties);
        }

        @Override
        public BitMask getListenedPropertyMask(Optional<Pattern> pattern, ObjectType objectType, List<String> settableProperties) {
            return AlphaNodeFieldConstraint.super.getListenedPropertyMask(pattern, objectType, settableProperties);
        }

        @Override
        public boolean equals(Object object, RuleBase kbase) {
            return AlphaNodeFieldConstraint.super.equals(object, kbase);
        }

        @Override
        public void registerEvaluationContext(RuleBuildContext ruleBuildContext) {
            AlphaNodeFieldConstraint.super.registerEvaluationContext(ruleBuildContext);
        }

        @Override
        public void mergeEvaluationContext(Constraint other) {
            AlphaNodeFieldConstraint.super.mergeEvaluationContext(other);
        }

        @Override
        public Collection<String> getPackageNames() {
            return AlphaNodeFieldConstraint.super.getPackageNames();
        }

        @Override
        public void addPackageNames(Collection<String> otherPkgs) {
            AlphaNodeFieldConstraint.super.addPackageNames(otherPkgs);
        }
    }

    public static abstract class MarkBetaConstraint implements BetaConstraints {
        @Override
        public ContextEntry[] createContext() {
            return new MarkContextEntry[]  {new MarkContextEntry()};
        }

        @Override
        public void updateFromTuple(ContextEntry[] contextEntries, ReteEvaluator reteEvaluator, Tuple tuple) {
            ((MarkContextEntry)contextEntries[0]).tp = tuple;
        }

        @Override
        public void updateFromFactHandle(ContextEntry[] contextEntries, ReteEvaluator reteEvaluator, FactHandle factHandle) {
            ((MarkContextEntry)contextEntries[0]).fh = factHandle;
        }

        @Override
        public boolean isAllowedCachedLeft(ContextEntry[] contextEntries, FactHandle factHandle) {
            return false;
        }

        @Override
        public boolean isAllowedCachedRight(ContextEntry[] contextEntries, Tuple tuple) {
            return false;
        }

        @Override
        public BetaNodeFieldConstraint[] getConstraints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BetaConstraints getOriginalConstraint() {
            return this;
        }

        @Override
        public boolean isIndexed() {
            return false;
        }

        @Override
        public int getIndexCount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public BetaMemory createBetaMemory(RuleBaseConfiguration ruleBaseConfiguration, short i) {
            return null;
        }

        @Override
        public void resetTuple(ContextEntry[] contextEntries) {

        }

        @Override
        public void resetFactHandle(ContextEntry[] contextEntries) {

        }

        @Override
        public BitMask getListenedPropertyMask(Pattern pattern, ObjectType objectType, List<String> list) {
            return null;
        }

        @Override
        public void init(BuildContext buildContext, short i) {

        }

        @Override
        public void initIndexes(int i, short i1, RuleBaseConfiguration ruleBaseConfiguration) {

        }

        @Override
        public BetaConstraints cloneIfInUse() {
            return null;
        }

        @Override
        public boolean isLeftUpdateOptimizationAllowed() {
            return false;
        }

        @Override
        public void registerEvaluationContext(BuildContext buildContext) {

        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {

        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        }
    }

    public static class MarkContextEntry implements ContextEntry {
        public Tuple tp;
        public FactHandle fh;

        @Override
        public ContextEntry getNext() {
            return null;
        }

        @Override
        public void setNext(ContextEntry contextEntry) {

        }

        @Override
        public void updateFromTuple(ValueResolver valueResolver, BaseTuple baseTuple) {
            this.tp = tp;
        }

        @Override
        public void updateFromFactHandle(ValueResolver valueResolver, FactHandle factHandle) {
            this.fh = fh;
        }

        public void isAllowedCachedLeft(FactHandle fh) {

        }

        public void isAllowedCachedRight(FactHandle fh) {

        }

        @Override
        public void resetTuple() {

        }

        @Override
        public void resetFactHandle() {

        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {

        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        }
    }

    public static class Person {
        private String name;

        public String getName() {
            return name;
        }
    }
}
