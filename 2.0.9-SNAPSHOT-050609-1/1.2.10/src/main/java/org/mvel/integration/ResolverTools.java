package org.mvel.integration;

/**
 * @author Christopher Brock
 */
public class ResolverTools {
    public static VariableResolverFactory appendFactory(VariableResolverFactory root, VariableResolverFactory newFactory) {
        VariableResolverFactory vrf = root;

        if (vrf.getNextFactory() == null) {
            vrf.setNextFactory(newFactory);
        }
        else {
            while (vrf.getNextFactory() != null) {
                vrf = vrf.getNextFactory();
            }
            vrf.setNextFactory(newFactory);
        }

        return newFactory;
    }

    public static <T extends VariableResolverFactory> T insertFactory(VariableResolverFactory root, T newFactory) {
        if (root.getNextFactory() == null) {
            root.setNextFactory(newFactory);
        }
        else {
            newFactory.setNextFactory(root.getNextFactory());
            root.setNextFactory(newFactory);
        }

        return newFactory;
    }
}
