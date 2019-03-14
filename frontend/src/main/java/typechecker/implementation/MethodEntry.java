package typechecker.implementation;

import ast.NodeList;
import ast.Type;
import util.ImpTable;
import util.List;

public class MethodEntry {

    private final MethodSignature methodSignature;
    private final ImpTable<Type> variables;
    private final ClassEntry classEntry; //parent class

    MethodEntry(MethodSignature methodSignature, ClassEntry classEntry) {
        this.methodSignature = methodSignature;
        this.classEntry = classEntry;
        variables = new ImpTable<Type>();
    }

    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    public ImpTable<Type> getVariables() {
        return variables;
    }

    public ClassEntry getClassEntry() {
        return classEntry;
    }

    void insertVariable(String variableName, Type variableType) throws ImpTable.DuplicateException {
        variables.put(variableName, variableType);
    }

    public Type lookupVariable(String name) {
        if (variables.lookup(name) != null) {
            return variables.lookup(name);
        } else {
            return classEntry.lookupField(name);
        }
    }

    static class MethodSignature {
        private Type returnType;
        private NodeList parameterTypes;

        MethodSignature(Type returnType, NodeList parameterTypes) {
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        public Type getReturnType() {
            return returnType;
        }

        public void setReturnType(Type returnType) {
            this.returnType = returnType;
        }

        public NodeList getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(NodeList parameterTypes) {
            this.parameterTypes = parameterTypes;
        }
    }
}
