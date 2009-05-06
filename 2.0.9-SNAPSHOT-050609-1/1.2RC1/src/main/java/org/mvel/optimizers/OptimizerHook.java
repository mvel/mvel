package org.mvel.optimizers;

import org.mvel.Accessor;

/**
 * @author Christopher Brock
 */
public interface OptimizerHook {
    /**
     * Should answer back whether or not this hook understands how to work with the current
     * optimizer.
     *
     * @param optimizer - class type of the current optimizer being used
     * @return boolean
     */
    public boolean isOptimizerSupported(Class<? extends AccessorOptimizer> optimizer);


    /**
     * The optimizer should delegate back to the hook through this method, passing an instance of itself
     * in the current state.
     *
     * @param optimizer - instance of optimizer
     * @return boolean
     */
    public Accessor generateAccessor(AccessorOptimizer optimizer);
}
