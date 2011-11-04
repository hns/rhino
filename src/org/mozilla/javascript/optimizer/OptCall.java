package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.Activation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

import java.util.Arrays;

public class OptCall extends ScriptableObject implements Activation {

    private Object[] locals;
    private Object[] originalArgs;  // actual arguments
    private Object[] args;          // copy-on-write args


    private int paramAndVarCount; // number of declared params + local vars
    private int localsStart; // index of first local (var or missing arg)
    private NativeFunction function;
    private Object arguments;

    private transient Activation parentActivation;

    final static int ARGUMENTS_ID = Integer.MAX_VALUE;

    public OptCall(NativeFunction function, Scriptable scope, Object[] args) {
        this.function = function;
        this.originalArgs = this.args = args;
        setParentScope(scope);
        // leave prototype null

        int paramCount = function.getParamCount();
        this.paramAndVarCount = function.getParamAndVarCount();
        this.localsStart = Math.min(args.length, paramCount);

        // Declared arguments not provided by the caller are treated as locals,
        // i.e. they can be accessed by name but not through the arguments object
        int localsLength = paramAndVarCount - localsStart;
        if (localsLength > 0) {
            locals = new Object[localsLength];
            Arrays.fill(locals, 0, localsLength, Undefined.instance);
        }
    }

    @Override
    public String getClassName() {
        return "Call";
    }

    public Object get(int index) {
        if (index >= 0 && index < localsStart) {
            return args[index];
        } else if (index >= localsStart && index < paramAndVarCount) {
            Object value = locals[index - localsStart];
            return value instanceof ConstHolder ?
                    ((ConstHolder)value).value : value;
        } else if (index == ARGUMENTS_ID) {
            return getArguments();
        } else {
            return Undefined.instance;
        }
    }

    public void set(int index, Object value) {
        if (index >= 0 && index < localsStart) {
            if (args == originalArgs) {
                copyArguments();
            }
            args[index] = value;
        } else if (index == ARGUMENTS_ID) {
            arguments = value == null ? UniqueTag.NULL_VALUE : value;
        } else if (index >= localsStart && index < paramAndVarCount) {
            int i = index - localsStart;
            if (!(locals[i] instanceof ConstHolder)) {
                locals[i] = value;
            }
        }
    }

    public void setConst(int index, Object value) {
        assert (index >= localsStart && index < paramAndVarCount);
        assert (locals[index - localsStart] == Undefined.instance);

        locals[index - localsStart] = new ConstHolder(value);
    }

    public Object getArguments() {
        if (arguments == UniqueTag.NULL_VALUE) {
            return null;
        }
        if (arguments == null) {
            synchronized (this) {
                if (arguments == null)
                    arguments = new Arguments();
            }
        }
        return arguments;
    }

    private synchronized void copyArguments() {
        if (args == originalArgs) {
            args = args.clone();
        }
    }

    public Object getFunction() {
        return function;
    }

    public void setParentActivation(Activation parentActivation) {
        this.parentActivation = parentActivation;
    }

    public Activation getParentActivation() {
        return parentActivation;
    }

    public class Arguments extends ScriptableObject {

        Object constructor, length, callee;
        boolean[] deleted;

        Arguments() {
            Scriptable parent = OptCall.this.getParentScope();
            setParentScope(parent);
            Scriptable scope = ScriptableObject.getTopLevelScope(parent);
            setPrototype(ScriptableObject.getObjectPrototype(scope));
            constructor = TopLevel.getBuiltinCtor(scope, TopLevel.Builtins.Object);
            length = Integer.valueOf(args.length);
            callee = function;
        }

        public String getClassName() {
            return "Arguments";
        }

        @Override
        public Object get(String name, Scriptable start) {
            Object value = super.get(name, start);
            if (value == NOT_FOUND) {
                if ("length".equals(name)) {
                    value = length;
                } else if ("constructor".equals(name)) {
                    value = constructor;
                } else if ("callee".equals(name)) {
                    value = callee;
                }
            }
            return value;
        }

        @Override
        public Object get(int index, Scriptable start) {
            if (hasArg(index)) {
                return args[index];
            } else {
                return super.get(index, start);
            }
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            if (hasArg(index)) {
                if (args == originalArgs) {
                    copyArguments();
                }
                args[index] = value;
            } else {
                super.put(index, start, value);
            }
        }

        @Override
        public boolean has(int index, Scriptable start) {
            return hasArg(index) || super.has(index, start);
        }

        @Override
        public void delete(int index) {
            if (index >= 0 && index < args.length) {
                if (deleted == null) {
                    createDeleted();
                }
                deleted[index] = true;
            } else {
                super.delete(index);
            }
        }

        @Override
        protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
            double d = ScriptRuntime.toNumber(id);
            int index = (int) d;
            if (d != index || !hasArg(index) || super.has(index, this)) {
                return super.getOwnPropertyDescriptor(cx, id);
            }
            Scriptable scope = getParentScope();
            if (scope == null) scope = this;
            return buildDataDescriptor(scope, args[index], EMPTY);
        }

        @Override
        public Object[] getIds() {
            int length = args.length;
            Object[] ids = new Object[length];
            for (int i = 0; i < length; i++) {
                ids[i] = Integer.valueOf(i);
            }
            return ids;
        }

        private boolean hasArg(int index) {
            return index >= 0 && index < args.length
                    && (deleted == null || !deleted[index]);
        }

        private synchronized void createDeleted() {
            if (deleted == null) {
                deleted = new boolean[args.length];
            }
        }
    }

}

class ConstHolder {
    final Object value;

    public ConstHolder(Object value) {
        this.value = value;
    }
}
