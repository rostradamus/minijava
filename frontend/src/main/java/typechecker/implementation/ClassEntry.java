package typechecker.implementation;

import ast.ClassDecl;
import ast.Type;
import util.FunTable;
import util.ImpTable;

import java.lang.reflect.Method;
import java.util.Map;

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

        ImpTable<Type> mergedFields = new ImpTable<>();
        if (superClass == null)
            return fields;
        for (Map.Entry<String, Type> t : superClass.getFields()) {
            mergedFields.set(t.getKey(), t.getValue());
        }
        for (Map.Entry<String, Type> t : fields) {
            mergedFields.set(t.getKey(), t.getValue());
        }
        return mergedFields;
    }

    public ImpTable<MethodEntry> getMethods() {
        ImpTable<MethodEntry> mergedMethods = new ImpTable<>();
        if (superClass == null)
            return methods;
        for (Map.Entry<String, MethodEntry> t : superClass.getMethods()) {
            mergedMethods.set(t.getKey(), t.getValue());
        }
        for (Map.Entry<String, MethodEntry> t : methods) {
            mergedMethods.set(t.getKey(), t.getValue());
        }
        return mergedMethods;
    }

    public ClassEntry getSuperClass() {
        return superClass;
    }

    public void setSuperClass(ClassEntry superClass) {
        this.superClass = superClass;
    }

    public Type lookupField(String name) {
        Type field = fields.lookup(name);
        if (field != null)
            return field;
        return superClass != null ? superClass.lookupField(name) : null;
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
