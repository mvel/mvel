package org.mvel2.ast;

import org.mvel2.ast.Function;
import org.mvel2.ast.ASTNode;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.compiler.ExecutableStatement;

import java.util.Map;
import java.util.HashMap;

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
        Receiver newReceiver = new Receiver(ReceiverType.PROPERTY, null);
        newReceiver.initValue = initCode;
        receivers.put(name, newReceiver);
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

    public class Receiver {
        private ReceiverType type;
        private Object receiver;
        private ExecutableStatement initValue;

        public Receiver(ReceiverType type, Object receiver) {
            this.type = type;
            this.receiver = receiver;
        }

        public void sendMessage(Object ctx, Object thisCtx, VariableResolverFactory factory, Message message) {
            switch (type) {
                case FUNCTION:
                    ((Function) receiver).call(ctx, thisCtx, factory, message.parameterValues);
                    break;
            }
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

    public class Message {
        public String[] parameterNames;
        public Object[] parameterValues;

        public Message(String[] parameterNames, Object[] parameterValues) {
            this.parameterNames = parameterNames;
            this.parameterValues = parameterValues;
        }
    }

    public enum ReceiverType {
        FUNCTION, MAPPED_METHOD, PROPERTY
    }

    public class ProtoInstance {
        private Proto protoType;
        private VariableResolverFactory instanceStates;

        public ProtoInstance(Proto protoType, Object ctx, Object thisCtx, VariableResolverFactory factory) {
            this.protoType = protoType;

            Map<String, Receiver> receivers = new HashMap<String, Receiver>();
            for (Map.Entry<String, Receiver> entry : protoType.receivers.entrySet()) {
                 receivers.put(entry.getKey(), entry.getValue().init(ctx, thisCtx, factory));
            }

            instanceStates = new MapVariableResolverFactory(receivers);
        }

        public void sendMessage(String name, Object ctx, Object thisCtx, VariableResolverFactory factory, Message message) {
            protoType.receivers.get(name).sendMessage(ctx, thisCtx, factory, message);
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
