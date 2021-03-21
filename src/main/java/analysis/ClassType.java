package analysis;

import java.util.*;

import notquitejava.ast.NQJClassDecl;
import notquitejava.ast.NQJFunctionDecl;
import notquitejava.ast.NQJVarDecl;

/**
 * Analysis dynamic representation of a AST class declaration.
 */
public class ClassType extends Type {
    /**
     * AST class declaration of the class.
     */
    private final NQJClassDecl classDecl;

    /**
     * Superclass of the class.
     */
    private final ClassType superclass;

    /**
     * Field index of the class, including such inherited.
     */
    private final Map<String, NQJVarDecl> fields;

    /**
     * Method index of the class, including such inherited.
     */
    private final Map<String, NQJFunctionDecl> methods;

    /**
     * Instantiate an Analysis dynamic representation of a AST class
     * declaration.
     *
     * @param classDecl   The AST class declaration to represent.
     * @param superclass  The superclass of the class declaration to represent.
     * @param fields      The field index of the class declaration, including
     *                    inheritance.
     * @param methods     The field index of the class declaration, including
     *                    inheritance.
     */
    public ClassType(
        NQJClassDecl classDecl,
        ClassType superclass,
        Map<String, NQJVarDecl> fields,
        Map<String, NQJFunctionDecl> methods
    ) {
        this.classDecl = classDecl;
        this.superclass = superclass;
        this.methods = methods;
        this.fields = fields;
    }

    /**
     * Checks whether this class is a subtype of another type.
     *
     * @param other  The potential supertype to check.
     *
     * @return  Whether this class is a subtype of the other type.
     */
    @Override
    boolean isSubtypeOf(Type other) {
        //
        // Type.ANY is a subtype of all types.
        //
        if (!(other instanceof ClassType)) {
            return other == ANY;
        }
        //
        // Check whether other is in the dependency tree.
        //
        ClassType classWalker = this;
        do {
            if (classWalker == other) {
                return true;
            }

            classWalker = classWalker.superclass;
        } while (classWalker != null);
        //
        // Otherwise, the types are compatible.
        //
        return false;
    }

    @Override
    public String toString() {
        return this.classDecl.getName();
    }

    /**
     * Gets the superclass of this class.
     *
     * @return null, or the superclass of this class, if existent.
     */
    public ClassType getSuperclass() {
        return this.superclass;
    }

    /**
     * Retrieves a field declaration of this class by its name.
     *
     * @param name  The name of the field declaration to retrieve.
     *
     * @return null, or the AST declaration of the field, if existent.
     */
    public NQJVarDecl getField(String name) {
        return this.fields.get(name);
    }

    /**
     * Gets the field index of this class, including inheritance.
     *
     * @return  The field index of this class, including inheritance.
     */
    public Map<String, NQJVarDecl> getFields() {
        return this.fields;
    }

    /**
     * Retrieves a method declaration of this class by its name.
     *
     * @param name  The name of the method declaration to retrieve.
     *
     * @return null, or the AST declaration of the method, if existent.
     */
    public NQJFunctionDecl getMethod(String name) {
        return this.methods.get(name);
    }

    /**
     * Gets the method index of this class, including inheritance.
     *
     * @return  The method index of this class, including inheritance.
     */
    public Map<String, NQJFunctionDecl> getMethods() {
        return this.methods;
    }

    /**
     * Gets the AST class declaration.
     *
     * @return  The AST class declaration.
     */
    public NQJClassDecl getDecl() {
        return this.classDecl;
    }
}
