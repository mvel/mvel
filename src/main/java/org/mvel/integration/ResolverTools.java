package org.mvel.integration;

/**
 * A set of tools for dealing with factorys, specifically to make chaining issues easy to deal with.
 *
 * @author Christopher Brock
 */
public class ResolverTools {
    /**
     * Based on a root factory, append the new factory to the end of the chain.
     *
     * @param root       The root factory
     * @param newFactory The new factory
     * @return An instance of the new factory
     */
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

    /**
     * Based on the root factory, insert the new factory right after the root, and before any other in the chain.
     *
     * @param root       The root factory
     * @param newFactory The new factory
     * @return An instance of the new factory.
     */
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
