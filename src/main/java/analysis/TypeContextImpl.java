package analysis;

import java.util.HashMap;
import java.util.Map;
import notquitejava.ast.NQJVarDecl;

/**
 * Implementation of a variable type context.
 * Manages VariableReferences, return types, this types.
 */
public class TypeContextImpl implements TypeContext {
    private final Map<String, VarRef> env;
    private Type returnType;
    private Type thisType;

    /**
     * Index of fields by name.
     */
    private final Map<String, VarRef> fields;

    /**
     * Saves reference to the env map constructor.
     */
    public TypeContextImpl(
        Map<String, VarRef> env,
        Map<String, VarRef> fields,
        Type returnType,
        Type thisType
    ) {
        this.env = env;
        this.returnType = returnType;
        this.thisType = thisType;
        //
        // CHANGE: Initialise fields.
        //
        this.fields = fields;
    }

    /**
     * Creates a new empty context with given return and this type.
     */
    public TypeContextImpl(Type returnType, Type thisType) {
        this.env = new HashMap<>();
        this.fields = new HashMap<>();
        this.returnType = returnType;
        this.thisType = thisType;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public Type getThisType() {
        return thisType;
    }

    @Override
    public VarRef lookupVar(String varUse) {
        return env.get(varUse);
    }

    @Override
    public void putVar(String varName, Type type, NQJVarDecl var) {
        this.env.put(varName, new VarRef(type, var));
    }

    /**
     * Gets a reference to a field previous stored to the context.
     *
     * @param fieldUse  The name of the field to retrieve.
     * @return  A reference to the requested field.
     */
    @Override
    public VarRef lookupField(String fieldUse) {
        return fields.get(fieldUse);
    }

    /**
     * Stores a field into the context.
     *
     * @param fieldName  The name of the field to store.
     * @param type       The type of the field to store.
     * @param field      The AST declaration of the field to store.
     */
    @Override
    public void putField(String fieldName, Type type, NQJVarDecl field) {
        this.fields.put(fieldName, new VarRef(type, field));
    }

    @Override
    public TypeContext copy() {
        //
        // CHANGE: Adapt to the new constructor.
        //
        return new TypeContextImpl(
            new HashMap<>(this.env),
            new HashMap<>(this.fields),
            this.returnType,
            this.thisType
        );
    }

    @Override
    public void setThisType(Type thisType) {
        this.thisType = thisType;
    }

    @Override
    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

}
