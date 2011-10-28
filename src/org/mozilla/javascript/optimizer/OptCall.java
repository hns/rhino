package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;

import java.util.Arrays;

public class OptCall extends ScriptableObject {

    private Object[] locals;
    private Object[] originalArgs;  // actual arguments
    private Object[] args;          // copy-on-write args

    private int paramCount; // number of declared parameters
    private int paramAndVarCount; // number of declared params + local vars
    private NativeFunction function;
    private Arguments arguments;

    public OptCall(NativeFunction function, Scriptable scope, Object[] args) {
        this.function = function;
        setParentScope(scope);
        // leave prototype null
        this.originalArgs = this.args = args;
        this.paramCount = function.getParamCount();
        this.paramAndVarCount = function.getParamAndVarCount();
        int localsLength = paramAndVarCount - paramCount;
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
        if (index >= 0 && index < paramCount) {
            return index < args.length ? args[index] : Undefined.instance;
        } else if (index >= paramCount && index < paramAndVarCount) {
            return locals[index - paramCount];
        } else if (index == -1) {
            return getArguments();
        } else {
            return Undefined.instance;
        }
    }

    public void set(int index, Object value) {
        if (index >= 0 && index < paramCount && index < args.length) {
            if (args == originalArgs) {
                copyArguments();
            }
            args[index] = value;
        } else if (index >= paramCount && index < paramAndVarCount) {
            // TODO check for const
            locals[index - paramCount] = value;
        }
    }

    private Arguments getArguments() {
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

    public class Arguments extends ScriptableObject {

        Object constructor, length, callee;

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

        // TODO: lots of stuff to do here

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
            if (index >= 0 && index < args.length) {
                return args[index];
            } else {
                return super.get(index, start);
            }
        }

        @Override
        public void put(int index, Scriptable start, Object value) {
            if (index >= 0 && index < args.length) {
                set(index, value);
            } else {
                super.put(index, start, value);
            }
        }

        @Override
        public boolean has(int index, Scriptable start) {
            return super.has(index, start) || (index >= 0 && index < args.length);
        }

        @Override
        public void delete(int index) {
            super.delete(index);
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
    }

}

