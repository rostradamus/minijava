package typechecker.implementation;

import ast.NodeList;
import ast.Type;
import util.ImpTable;

public class MethodEntry {

    private Type returnType;
    private NodeList parameterTypes;
    private final ClassEntry classEntry;
    public final ImpTable<Type> variables;

    MethodEntry(Type _returnType, NodeList _parameterTypes, ClassEntry _classEntry) {
        this.returnType = _returnType;
        this.parameterTypes = _parameterTypes;
        this.classEntry = _classEntry;
        variables = new ImpTable<>();
    }

    public ImpTable<Type> getVariables() {
        return variables;
    }

    public ClassEntry getClassEntry() {
        return classEntry;
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

    public Type lookupVariable(String name) {
        if (variables.lookup(name) != null) {
            return variables.lookup(name);
        } else {
            return classEntry.lookupField(name);
        }
    }
}
