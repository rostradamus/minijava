package typechecker.implementation;

import ast.Type;
import util.ImpTable;

import java.lang.reflect.Method;

public class ClassEntry {
    public final String name;
    public final ImpTable<MethodEntry> methods;
    public final ImpTable<Type> fields;
    private ClassEntry superClass;

    ClassEntry(String _name, ImpTable<Type> _fields, ImpTable<MethodEntry> _methods) {
        this.name = _name;
        this.fields = _fields;
        this.methods = _methods;
        this.superClass = null;
    }

    public String getName() {
        return name;
    }

    public ImpTable<Type> getFields() {
        return fields;
    }

    public ImpTable<MethodEntry> getMethods() {
        return methods;
    }

    public ClassEntry getSuperClass() {
        return superClass;
    }

    public void setSuperClass(ClassEntry superClass) {
        this.superClass = superClass;
    }

    public Type lookupField(String name) {
        return fields.lookup(name);
    }

    public MethodEntry lookupMethod(String name) {
        if (methods.lookup(name) != null) {
            return methods.lookup(name);
        } else {
            return superClass != null ? superClass.lookupMethod(name) : null;
        }
    }

    boolean containsMethod(String methodName) {
        // lookup within this class method entry table
        if (methods.lookup(methodName) != null) {
            return true;
        }

        // If you can't find it, look in the Super class if it exists
        if (superClass != null) {
            superClass.containsMethod(methodName);
        }

        // Doesn't exist
        return false;
    }
}
