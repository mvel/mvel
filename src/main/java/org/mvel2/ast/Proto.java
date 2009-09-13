package org.mvel2.ast;

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.util.CallableProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

public class Proto extends ASTNode {
    private String name;
    private Map<String, Receiver> receivers;

    public Proto(String name) {
        this.name = name;
        this.receivers = new HashMap<String, Receiver>();
    }

    public void declareReceiver(String name, Function function) {
        receivers.put(name, new Receiver(ReceiverType.FUNCTION, function));
    }

    public void declareReceiver(String name, Class type, ExecutableStatement initCode) {        
        receivers.put(name, new Receiver(ReceiverType.PROPERTY, initCode));
    }

    public ProtoInstance newInstance(Object ctx, Object thisCtx, VariableResolverFactory factory) {
        return new ProtoInstance(this, ctx, thisCtx, factory);
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, this);
        return this;
    }

    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, this);
        return this;
    }

    public class Receiver implements CallableProxy {
        private ReceiverType type;
        private Object receiver;
        private ExecutableStatement initValue;

        public Receiver(ReceiverType type, Object receiver) {
            this.type = type;
            this.receiver = receiver;
        }

        public Receiver(ReceiverType type, ExecutableStatement stmt) {
            this.type = type;
            this.initValue = stmt;
        }

        public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parms) {
            switch (type) {
                case FUNCTION:
                    return ((Function) receiver).call(ctx, thisCtx, factory, parms);
                case PROPERTY:
                    return receiver;
            }
            return null;
        }

        public void setInitValue(ExecutableStatement stmt) {
            initValue = stmt;
        }

        public Receiver init(Object ctx, Object thisCtx, VariableResolverFactory factory) {
            return new Receiver(type,
                    type == ReceiverType.PROPERTY && initValue != null ? initValue.getValue(ctx, thisCtx, factory) :
                            receiver);
        }
    }

    public enum ReceiverType {
        FUNCTION, MAPPED_METHOD, PROPERTY
    }

    public class ProtoInstance implements Map<String, Receiver> {
        private Proto protoType;
        private VariableResolverFactory instanceStates;
        private Map<String, Receiver> receivers;

        public ProtoInstance(Proto protoType, Object ctx, Object thisCtx, VariableResolverFactory factory) {
            this.protoType = protoType;

            receivers = new HashMap<String, Receiver>();
            for (Map.Entry<String, Receiver> entry : protoType.receivers.entrySet()) {
                receivers.put(entry.getKey(), entry.getValue().init(ctx, thisCtx, factory));
            }

            instanceStates = new MapVariableResolverFactory(receivers);
        }

        public int size() {
            return receivers.size();
        }

        public boolean isEmpty() {
            return receivers.isEmpty();
        }

        public boolean containsKey(Object key) {
            return receivers.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return receivers.containsValue(value);
        }

        public Receiver get(Object key) {
            return receivers.get(key);
        }

        public Receiver put(String key, Receiver value) {
            return receivers.put(key, value);
        }

        public Receiver remove(Object key) {
            return receivers.remove(key);
        }

        public void putAll(Map m) {
        }

        public void clear() {
        }

        public Set<String> keySet() {
            return receivers.keySet();
        }

        public Collection<Receiver> values() {
            return receivers.values();
        }

        public Set<Entry<String,Receiver>> entrySet() {
            return receivers.entrySet();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "proto " + name;
    }
}
