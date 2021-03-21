package analysis;

import notquitejava.ast.NQJVarDecl;

/**
 * Type context for variable look ups.
 */
public interface TypeContext {
    Type getReturnType();

    Type getThisType();

    void setThisType(Type thisType);

    void setReturnType(Type returnType);

    VarRef lookupVar(String varUse);

    void putVar(String varName, Type type, NQJVarDecl var);

    //
    // CHANGE: Introduce separate field storage to enable field shadowing while
    //         disallowing variable shadowing.
    //

    /**
     * Gets a reference to a field previous stored to the context.
     *
     * @param fieldUse  The name of the field to retrieve.
     * @return  A reference to the requested field.
     */
    VarRef lookupField(String fieldUse);

    /**
     * Stores a field into the context.
     *
     * @param fieldName  The name of the field to store.
     * @param type       The type of the field to store.
     * @param field      The AST declaration of the field to store.
     */
    void putField(String fieldName, Type type, NQJVarDecl field);

    TypeContext copy();

    /**
     * Variable declaration and type wrapper class.
     */
    class VarRef {
        final NQJVarDecl decl;
        final Type type;

        public VarRef(Type type, NQJVarDecl decl) {
            this.decl = decl;
            this.type = type;
        }
    }
}
