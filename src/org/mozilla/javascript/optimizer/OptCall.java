/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Hannes Wallnoefer
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

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
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A function activation object that eliminates hash lookups for parameter
 * and local variable access. This works in conjunction with {@link Codegen}
 * resolving the depth and index of bound names at compile time, and
 * {@link OptRuntime} providing optimized methods to access these names.</p>
 *
 * <p>{@code OptCall} optimization is used when a function requires activation
 * (for instance because there are nested functions) but does not contain calls
 * to {@code eval()}, since {@code eval()} may allocate variables in the local
 * function scope that are not known at compile time.</p>
 *
 * <p>Although this class implements some some of the methods defined in the
 * {@link Scriptable} interface, the only case in which those should be used
 * is for interpreted {@code eval()} code in nested functions.</p>
 *
 * @author Hannes Wallnoefer
 */
public class OptCall extends ScriptableObject implements Activation {

    private Object[] locals;        // local variables
    private Object[] originalArgs;  // actual original arguments
    private Object[] args;          // copy-on-write args


    private int paramAndVarCount; // number of declared params + local vars
    private int localsStart;      // index of first local (var or missing arg)
    private NativeFunction function;
    private transient Object arguments;     // the arguments object

    // Created on demand to support traditional Scriptable interface
    // for eval code or other interpreted code. This should only ever be
    // initialized for eval() code in nested functions accessing bindings in
    // this function.
    private transient Map<String, Integer> propertyMap;

    private transient Activation parentActivation;

    public final static int ARGUMENTS_ID = Integer.MAX_VALUE;

    public OptCall(NativeFunction function, Scriptable scope, Object[] args) {
        this.function = function;
        this.originalArgs = this.args = args;
        setParentScope(scope);
        // leave prototype null

        int paramCount = function.getParamCount();
        this.paramAndVarCount = function.getParamAndVarCount();

        // Declared arguments not provided by the caller are treated as locals,
        // i.e. they can be accessed by name but not through the arguments object
        this.localsStart = Math.min(args.length, paramCount);
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

    /**
     * Get the value of a parameter or local variable in this activation object.
     * @param index the parameter or variable index
     * @return the
     */
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

    /**
     * Set a parameter or local variable in this activation object.
     * @param index the parameter or variable index
     * @param value the value
     */
    public void set(int index, Object value) {
        if (index >= 0 && index < localsStart) {
            if (args == originalArgs) {
                copyArgs();
            }
            args[index] = value;
        } else if (index >= localsStart && index < paramAndVarCount) {
            int i = index - localsStart;
            if (!(locals[i] instanceof ConstHolder)) {
                locals[i] = value;
            }
        } else if (index == ARGUMENTS_ID) {
            arguments = value == null ? UniqueTag.NULL_VALUE : value;
        }
    }

    /**
     * Set a constant in this activation object.
     * @param index the constant index
     * @param value the constant value
     */
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

    private synchronized void copyArgs() {
        if (args == originalArgs) {
            args = args.clone();
        }
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (propertyMap == null) initPropertyMap();
        Integer index = propertyMap.get(name);
        if (index != null) {
            return get(index.intValue());
        }
        return super.get(name, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (propertyMap == null) initPropertyMap();
        return propertyMap.containsKey(name) || super.has(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (propertyMap == null) initPropertyMap();
        Integer index = propertyMap.get(name);
        if (index != null) {
            set(index.intValue(), value);
        } else {
            super.put(name, start, value);
        }
    }

    private synchronized void initPropertyMap() {
        // Create a name->index map that allows us to implement methods of
        // the Scriptable interface. Note that these are only for interpreted
        // eval() code and should never be called by optimized code.
        if (propertyMap == null) {
            Map<String,Integer> map = new HashMap<String, Integer>();
            int paramCount = function.getParamCount();
            for (int i = 0; i < paramCount; i++) {
                map.put(function.getParamOrVarName(i), Integer.valueOf(i));
            }
            // Only add arguments object if it isn't shadowed by a parameter
            // with the same name
            if (!map.containsKey("arguments")) {
                map.put("arguments", ARGUMENTS_ID);
            }
            for (int i = paramCount; i < paramAndVarCount; i++) {
                map.put(function.getParamOrVarName(i), Integer.valueOf(i));
            }
            propertyMap = map;
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
                    copyArgs();
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
