package analysis;

import java.util.*;
import notquitejava.ast.*;

/**
 * Name table for analysis class hierarchies.
 */
public class NameTable {
    private final Map<Type, ArrayType> arrayTypes = new HashMap<>();

    private final Map<String, NQJFunctionDecl> globalFunctions = new HashMap<>();

    private final Analysis analysis;

    //
    // CHANGE: Implement class indexing analogous to array and function indices.
    //

    /**
     * An index of all AST class declarations by name.
     */
    private final Map<String, NQJClassDecl> globalClasses = new HashMap<>();

    /**
     * A cache of all class Analysis dynamic representations by name.
     */
    private final Map<String, ClassType> classTypes = new HashMap<>();

    /**
     * A list of already requested classes for usage in getClassType().
     */
    private ArrayList<NQJClassDecl> getClassTypeRecurseDecls = new ArrayList<>();

    NameTable(Analysis analysis, NQJProgram prog) {
        this.analysis = analysis;
        globalFunctions.put("printInt", NQJ.FunctionDecl(NQJ.TypeInt(), "main",
                NQJ.VarDeclList(NQJ.VarDecl(NQJ.TypeInt(), "elem")), NQJ.Block()));
        for (NQJFunctionDecl f : prog.getFunctionDecls()) {
            var old = globalFunctions.put(f.getName(), f);
            if (old != null) {
                analysis.addError(f, "There already is a global function with name " + f.getName()
                        + " defined in " + old.getSourcePosition());
            }
        }

        //
        // CHANGE: Implement class indexing.
        //

        for (NQJClassDecl c : prog.getClassDecls()) {
            var old = globalClasses.put(c.getName(), c);
            if (old != null) {
                analysis.addError(c, "A class with name " + c.getName()
                        + " is already defined in " + old.getSourcePosition() + ".");
            }
        }
    }

    public NQJFunctionDecl lookupFunction(String functionName) {
        return globalFunctions.get(functionName);
    }

    /**
     * Look up an AST class declaration by name.
     *
     * @param className  The name of the class to look up.
     *
     * @return null, or the AST class declaration, if existent.
     */
    public NQJClassDecl lookupClass(String className) {
        return globalClasses.get(className);
    }

    /**
     * Transform base type to array type.
     */
    public ArrayType getArrayType(Type baseType) {
        if (!arrayTypes.containsKey(baseType)) {
            arrayTypes.put(baseType, new ArrayType(baseType));
        }
        return arrayTypes.get(baseType);
    }

    /**
     * Determines whether two methods are compatible, i.e. whether they have the
     * same return and parameter types).
     *
     * @param a  The first method to check.
     * @param b  The second method to check.
     *
     * @return  Whether the two methods are compatible.
     */
    private boolean methodsCompatible(NQJFunctionDecl a, NQJFunctionDecl b) {
        if (!a.getReturnType().structuralEquals(b.getReturnType())) {
            return false;
        }
        //
        // Compare the parameters individually as they do not need to have the
        // same name to be compatible.
        //
        final int numParams = a.getFormalParameters().size();
        if (numParams != b.getFormalParameters().size()) {
            return false;
        }

        for (int i = 0; i < numParams; ++i) {
            final NQJVarDecl paramA = a.getFormalParameters().get(i);
            final NQJVarDecl paramB = b.getFormalParameters().get(i);
            if (!paramA.getType().structuralEquals(paramB.getType())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieve an Analysis dynamic representation of an AST class declaration.
     *
     * <p>The dynamic representation indexes all fields and methods of the
     * class declaration, include those inherited. If the class is part of an
     * inheritance cycle, it contains all fields and methods of all classes in
     * the cycle. This allows the name and type analysis to continue.
     *
     * <p>This method implicitly vists the class inheritance declaration and its
     * field declarations, errors are emitted appropriately.
     *
     * @param classDecl  The AST class declaration to convert.
     *
     * @return  The Analysis dynamic representation of the class.
     */
    public ClassType getClassType(NQJClassDecl classDecl) {
        final String className = classDecl.getName();
        //
        // Return the chached instance, if existent.
        //
        final ClassType cachedClassType = classTypes.get(className);
        if (cachedClassType != null) {
            return cachedClassType;
        }
        //
        // Bookkeep the classes requested for indexing to detect cycles. This
        // must be a list as the root class may not be part of the cycle itself.
        //
        final boolean classCycle = getClassTypeRecurseDecls.contains(classDecl);
        if (!classCycle) {
            getClassTypeRecurseDecls.add(classDecl);
        } else {
            analysis.addError(
                classDecl,
                "Class " + className + " is part of an inheritance cycle."
            );
        }

        final HashMap<String, NQJFunctionDecl> methods;
        final ClassType superclass;
        final HashMap<String, NQJVarDecl> fields;
        //
        // Force this class index to not inherit from a superclass if a cycle
        // was encountered. The recursive root call will reconstruct this class
        // importing from all superclasses, which will yield a complete index of
        // fields and methods to avoid False Positives in error generation.
        //
        if (classDecl.getExtended() instanceof NQJExtendsNothing || classCycle) {
            superclass = null;
            fields = new HashMap<>();
            methods = new HashMap<>();
        } else {
            NQJExtendsClass extendsClass = (NQJExtendsClass) classDecl.getExtended();
            superclass = getClassType(extendsClass.getName());
            if (superclass != null) {
                fields = new HashMap<>(superclass.getFields());
                methods = new HashMap<>(superclass.getMethods());
                //
                // As implciit class declaration Visitor, populate inheritance
                // information.
                //
                classDecl.setDirectSuperClass(superclass.getDecl());
            } else {
                analysis.addError(
                    classDecl,
                    "Superclass " + extendsClass.getName()
                    + " cannot be found for class " + classDecl.getName()
                );
                fields = new HashMap<>();
                methods = new HashMap<>();
            }
        }
        //
        // The inheritance path has been completed, terminate the recursion.
        //
        getClassTypeRecurseDecls.clear();
        //
        // Add all fields of the class to the index. Implicitly visists and
        // accepts all fields declarations by adding errors appropriately.
        //
        final Map<String, NQJVarDecl> fieldIndex = new HashMap<>();
        for (final NQJVarDecl fieldDecl : classDecl.getFields()) {
            final NQJType fieldDecType = fieldDecl.getType();
            if (!analysis.typeResolvable(fieldDecType)) {
                analysis.addUnresolvedTypeError(fieldDecl, fieldDecType);
            }

            final String fieldName = fieldDecl.getName();
            final NQJVarDecl oldDecl = fieldIndex.get(fieldName);
            if (oldDecl != null) {
                analysis.addError(
                    fieldDecl,
                    "A field with name " + fieldName + " is already defined in "
                      + oldDecl.getSourcePosition() + "."
                );
            }
            fieldIndex.put(fieldName, fieldDecl);
            fields.put(fieldName, fieldDecl);
        }
        //
        // Add all methods of the class to the index. Makes sure there are no
        // duplicate methods, but does not perform a full analysis (this is done
        // by the general Visitor).
        //
        final Map<String, NQJFunctionDecl> methodIndex = new HashMap<>();
        for (final NQJFunctionDecl methodDecl : classDecl.getMethods()) {
            final String methodName = methodDecl.getName();
            NQJFunctionDecl oldDecl = methodIndex.get(methodName);
            if (oldDecl != null) {
                analysis.addError(methodDecl, "A method with name " + methodDecl.getName()
                    + " is already defined in " + oldDecl.getSourcePosition() + ".");
            } else {
                oldDecl = methods.get(methodName);
                if (oldDecl != null && !methodsCompatible(methodDecl, oldDecl)) {
                    analysis.addError(
                        methodDecl,
                        "A method with name " + methodName
                          + " and different type is already defined in "
                          + oldDecl.getSourcePosition() + "."
                    );
                }
            }
            methodIndex.put(methodName, methodDecl);
            methods.put(methodName, methodDecl);
        }

        final ClassType classType = new ClassType(classDecl, superclass, fields, methods);
        //
        // When classCycle is true, this instance will be temporary to correctly
        // propagate fields and methods to all subclasses. It will be
        // reconstructed by the root call.
        //
        if (!classCycle) {
            classTypes.put(className, classType);
        }

        return classType;
    }

    /**
     * Retrieve an Analysis dynamic representation of an AST class declaration.
     *
     * <p>The dynamic representation indexes all fields and methods of the
     * class declaration, include those inherited. If the class is part of an
     * inheritance cycle, it contains all fields and methods of all classes in
     * the cycle. This allows the name and type analysis to continue.
     *
     * <p>This method implicitly vists the class inheritance declaration and its
     * field declarations, errors are emitted appropriately.
     *
     * @param className  The name of the AST class declaration to convert.
     *
     * @return  null, or the Analysis dynamic representation of the class, if
     *          existent.
     */
    public ClassType getClassType(String className) {
        final NQJClassDecl classDecl = lookupClass(className);
        if (classDecl == null) {
            return null;
        }

        return getClassType(classDecl);
    }
}
