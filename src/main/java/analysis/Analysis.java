package analysis;

import java.util.*;
import notquitejava.ast.*;


/**
 * Analysis visitor to handle most of the type rules specified to NQJ.
 */
public class Analysis extends NQJElement.DefaultVisitor {

    private final NQJProgram prog;
    private final List<TypeError> typeErrors = new ArrayList<>();
    private NameTable nameTable;
    private final LinkedList<TypeContext> ctxt = new LinkedList<>();

    public void addError(NQJElement element, String message) {
        typeErrors.add(new TypeError(element, message));
    }

    public Analysis(NQJProgram prog) {
        this.prog = prog;
    }

    /**
     * Checks the saves NQJProgram for type errors.
     * Main entry point for type checking.
     */
    public void check() {
        nameTable = new NameTable(this, prog);

        verifyMainMethod();

        prog.accept(this);
    }

    private void verifyMainMethod() {
        var main = nameTable.lookupFunction("main");
        if (main == null) {
            typeErrors.add(new TypeError(prog, "Method int main() must be present"));
            return;
        }
        if (!(main.getReturnType() instanceof NQJTypeInt)) {
            typeErrors.add(new TypeError(main.getReturnType(),
                    "Return type of the main method must be int"));
        }
        if (!(main.getFormalParameters().isEmpty())) {
            typeErrors.add(new TypeError(main.getFormalParameters(),
                    "Main method does not take parameters"));
        }
        // Check if return statement is there as the last statement
        NQJStatement last = null;
        for (NQJStatement nqjStatement : main.getMethodBody()) {
            last = nqjStatement;
        }
        // TODO this forces the main method to have a single return at the end
        //      instead check for all possible paths to end in a return statement
        if (!(last instanceof NQJStmtReturn)) {
            typeErrors.add(new TypeError(main.getFormalParameters(),
                    "Main method does not have a return statement as the last statement"));
        }
    }

    @Override
    public void visit(NQJFunctionDecl m) {
        // parameter names are unique, build context
        //
        // CHANGE: Initialize ThisType as Type.ANY to allow for gracious name
        //         and type analysis.
        //
        TypeContext mctxt = this.ctxt.isEmpty()
                ? new TypeContextImpl(null, Type.ANY)
                : this.ctxt.peek().copy();
        Set<String> paramNames = new HashSet<>();
        for (NQJVarDecl v : m.getFormalParameters()) {
            if (!paramNames.add(v.getName())) {
                //
                // CHANGE: Move the error from the method to the parameter declaration.
                //
                addError(v, "Parameter with name " + v.getName() + " already exists.");
            }
            //
            // CHANGE: Add unresolved type error-checking.
            //
            final NQJType paramDecType = v.getType();
            final Type paramType = graciousType(v, paramDecType, "Parameter");
            mctxt.putVar(v.getName(), paramType, v);
        }
        //
        // CHANGE: Add unresolved type error-checking.
        //
        final NQJType retValDecType = m.getReturnType();
        final Type retValType = graciousType(m, retValDecType, "Return");

        mctxt.setReturnType(retValType);
        // enter method context
        ctxt.push(mctxt);

        m.getMethodBody().accept(this);

        // exit method context
        ctxt.pop();
    }



    @Override
    public void visit(NQJStmtReturn stmtReturn) {
        Type actualReturn = checkExpr(ctxt.peek(), stmtReturn.getResult());
        Type expectedReturn = ctxt.peek().getReturnType();
        if (!actualReturn.isSubtypeOf(expectedReturn)) {
            addError(stmtReturn, "Should return value of type " + expectedReturn
                    + ", but found " + actualReturn + ".");
        }
    }

    @Override
    public void visit(NQJStmtAssign stmtAssign) {
        Type lt = checkExpr(ctxt.peek(), stmtAssign.getAddress());
        Type rt = checkExpr(ctxt.peek(), stmtAssign.getValue());
        if (!rt.isSubtypeOf(lt)) {
            addError(stmtAssign.getValue(), "Cannot assign value of type " + rt
                    + " to " + lt + ".");
        }
    }

    @Override
    public void visit(NQJStmtExpr stmtExpr) {
        checkExpr(ctxt.peek(), stmtExpr.getExpr());
    }

    @Override
    public void visit(NQJStmtWhile stmtWhile) {
        Type ct = checkExpr(ctxt.peek(), stmtWhile.getCondition());
        if (!ct.isSubtypeOf(Type.BOOL)) {
            addError(stmtWhile.getCondition(),
                    "Condition of while-statement must be of type boolean, but this is of type "
                            + ct + ".");
        }
        super.visit(stmtWhile);
    }

    @Override
    public void visit(NQJStmtIf stmtIf) {
        Type ct = checkExpr(ctxt.peek(), stmtIf.getCondition());
        if (!ct.isSubtypeOf(Type.BOOL)) {
            addError(stmtIf.getCondition(),
                    "Condition of if-statement must be of type boolean, but this is of type "
                            + ct + ".");
        }
        super.visit(stmtIf);
    }

    @Override
    public void visit(NQJBlock block) {
        TypeContext bctxt = this.ctxt.peek().copy();
        for (NQJStatement s : block) {
            // could also be integrated into the visitor run
            if (s instanceof NQJVarDecl) {
                NQJVarDecl varDecl = (NQJVarDecl) s;
                TypeContextImpl.VarRef ref = bctxt.lookupVar(varDecl.getName());
                if (ref != null) {
                    addError(varDecl, "A variable with name " + varDecl.getName()
                            + " is already defined.");
                }
                //
                // CHANGE: Add unresolved type error-checking.
                //
                final NQJType varDecType = varDecl.getType();
                final Type varType = graciousType(varDecl, varDecType);
                bctxt.putVar(varDecl.getName(), varType, varDecl);
            } else {
                // enter block context
                ctxt.push(bctxt);
                s.accept(this);
                // exit block context
                ctxt.pop();
            }
        }
    }

    /**
     * Visit and accept a NQJ class declaration, including fields and methods.
     *
     * <p>This method performs extensive name and type analysis (including
     * error generation for unresolved types, duplicate symbols, etc.). No added
     * analysis errors guarantees the NQJ class declaration including all its
     * fields and methods is valid.
     *
     * @param classDecl  The NQJ class declaration to visit and accept.
     */
    @Override
    public void visit(NQJClassDecl classDecl) {
        //
        // Initialize ThisType as Type.ANY to allow for gracious name and type
        // analysis.
        //
        final TypeContext cctxt = new TypeContextImpl(null, Type.ANY);
        //
        // This call cannot return null as it is always a root call (null can
        // only be returned for recursive cases).
        //
        final ClassType classType = nameTable.getClassType(classDecl);
        cctxt.setThisType(classType);
        //
        // Fields have their own storage, as in contrast to variables, they are
        // allowed to be shadowed.
        //
        for (final String fieldName : classType.getFields().keySet()) {
            final NQJVarDecl varDecl = classType.getFields().get(fieldName);
            final Type varType = graciousType(varDecl.getType());
            cctxt.putField(varDecl.getName(), varType, varDecl);
        }
        //
        //
        //
        ctxt.push(cctxt);
        //
        // Inheritance and fields are implicitly visited by getClassType().
        //
        classDecl.getMethods().accept(this);

        ctxt.pop();
    }

    @Override
    public void visit(NQJVarDecl varDecl) {
        //
        // CHANGE: Add NQJClassDecl to the list.
        // This type is implicitly handled by NQJBlock, NQJClassDecl, and
        // NQJFunctionDecl.
        //
        throw new IllegalStateException();
    }

    /**
     * Visit and accept an 'extends nothing' declaration of a class.
     *
     * @param extendsNothing  The declaration to visit and accept.
     */
    @Override
    public void visit(NQJExtendsNothing extendsNothing) {
        //
        // This type is implicitly handled by NQJClassDecl.
        //
        throw new IllegalStateException();
    }

    /**
     * Visit and accept an 'extends class' declaration of a class.
     *
     * @param extendsClass  The declaration to visit and accept.
     */
    @Override
    public void visit(NQJExtendsClass extendsClass) {
        //
        // This type is implicitly handled by NQJClassDecl.
        //
        throw new IllegalStateException();
    }

    public Type checkExpr(TypeContext ctxt, NQJExpr e) {
        return e.match(new ExprChecker(this, ctxt));
    }

    public Type checkExpr(TypeContext ctxt, NQJExprL e) {
        return e.match(new ExprChecker(this, ctxt));
    }

    /**
     * NQJ AST element to Type converter.
     */
    public Type type(NQJType type) {
        //
        // CHANGE: If already set, return the cached dynamic representation.
        //
        Type result = type.getType();
        if (result != null) {
            return result;
        }

        result =  type.match(new NQJType.Matcher<>() {


            @Override
            public Type case_TypeBool(NQJTypeBool typeBool) {
                return Type.BOOL;
            }

            @Override
            public Type case_TypeClass(NQJTypeClass typeClass) {
                //
                // CHANGE: Implement class resolution.
                //
                final ClassType classType = nameTable.getClassType(typeClass.getName());
                if (classType == null) {
                    return Type.INVALID;
                }

                return classType;
            }

            @Override
            public Type case_TypeArray(NQJTypeArray typeArray) {
                Type type = type(typeArray.getComponentType());
                //
                // CHANGE: Propagate unresolved type status.
                //
                if (type != Type.INVALID) {
                    type = nameTable.getArrayType(type);
                }

                return type;
            }

            @Override
            public Type case_TypeInt(NQJTypeInt typeInt) {
                return Type.INT;
            }

        });

        type.setType(result);
        return result;
    }

    public NameTable getNameTable() {
        return nameTable;
    }

    public List<TypeError> getTypeErrors() {
        return new ArrayList<>(typeErrors);
    }

    /**
     * Unwraps the AST type down to a non-array type.
     *
     * @param type  the NQJ type to unwrap.
     *
     * @return  The non-array type unwrapped from the type.
     */
    private NQJType unwrapArrayType(NQJType type) {
        while (type instanceof NQJTypeArray) {
            final NQJTypeArray arrayType = (NQJTypeArray) type;
            type = arrayType.getComponentType();
        }

        return type;
    }

    /**
     * Add an 'unresolved type' error to an AST element.
     *
     * @param e         The AST element to annotate.
     * @param typeName  The name of the type that cannot be resolved.
     * @param prefix    null, or the prefix to prepend to the error message.
     */
    public void addUnresolvedTypeError(NQJElement e, String typeName, String prefix) {
        prefix = (prefix == null ? "Type " : prefix + " type ");
        addError(e, prefix + typeName + " cannot be resolved.");
    }

    /**
     * Add an 'unresolved type' error to an AST element.
     *
     * @param e         The AST element to annotate.
     * @param typeName  The name of the type that cannot be resolved.
     */
    public void addUnresolvedTypeError(NQJElement e, String typeName) {
        addUnresolvedTypeError(e, typeName, null);
    }

    /**
     * Add an 'unresolved type' error to an AST element.
     *
     * @param e       The AST element to annotate.
     * @param type    The AST type that cannot be resolved.
     * @param prefix  null, or the prefix to prepend to the error message.
     */
    public void addUnresolvedTypeError(NQJElement e, NQJType type, String prefix) {
        //
        // The only unresolved types as of now are classes.
        //
        final NQJTypeClass undefClassType = (NQJTypeClass) unwrapArrayType(type);
        addUnresolvedTypeError(e, undefClassType.getName(), prefix);
    }

    /**
     * Add an 'unresolved type' error to an AST element.
     *
     * @param e     The AST element to annotate.
     * @param type  The AST type that cannot be resolved.
     */
    public void addUnresolvedTypeError(NQJElement e, NQJType type) {
        addUnresolvedTypeError(e, type, null);
    }

    /**
     * Verifies whether an AST type is resolvable.
     *
     * @param type  The AST type to check.
     *
     * @return  Whether the AST type is resolvable.
     */
    public boolean typeResolvable(NQJType type) {
        //
        // Unwind the array type as resolvability depends on the base type.
        //
        final NQJType unwrappedType = unwrapArrayType(type);
        //
        // All non-class types can always be resolved.
        //
        if (!(unwrappedType instanceof NQJTypeClass)) {
            return true;
        }
        //
        // This assumes that the existance of a declaration implies
        // resolvability, which currently is the case.
        //
        final NQJTypeClass classType = (NQJTypeClass) unwrappedType;
        return nameTable.lookupClass(classType.getName()) != null;
    }

    /**
     * Graciously converts an AST type to its Analysis dynamic representation.
     *
     * <p>Type.ANY is returned for unresolvable types, so that name and type
     * analysis can progress further.
     *
     * @param type  The AST type to convert.
     *
     * @return Type.ANY, or the Analysis dynamic representation of the AST
     *         type, if it is resolvable.
     */
    public Type graciousType(NQJType type) {
        final Type resolvedType = type(type);
        if (resolvedType == Type.INVALID) {
            //
            // Let the name and type analysis continue graciously.
            //
            return Type.ANY;
        }

        return resolvedType;
    }

    /**
     * Graciously converts an AST type to its Analysis dynamic representation.
     *
     * <p>If a type cannot be resolved, an appropriate error message is emitted.
     * Type.ANY is returned for unresolvable types, so that name and type
     * analysis can progress further.
     *
     * @param e       The AST element the type is referenced it.
     * @param type    The AST type to convert.
     * @param prefix  null, or the prefix to prepend to the error messages.
     *
     * @return  Type.ANY, or the Analysis dynamic representation of the AST
     *          type, if it is resolvable.
     */
    public Type graciousType(NQJElement e, NQJType type, String prefix) {
        final Type resolvedType = graciousType(type);
        if (resolvedType == Type.ANY) {
            addUnresolvedTypeError(e, type, prefix);
            //
            // Let the name and type analysis continue graciously.
            //
        }

        return resolvedType;
    }

    /**
     * Graciously converts an AST type to its Analysis dynamic representation.
     *
     * <p>If a type cannot be resolved, an appropriate error message is emitted.
     * Type.ANY is returned for unresolvable types, so that name and type
     * analysis can progress further.
     *
     * @param e     The AST element the type is referenced it.
     * @param type  The AST type to convert.
     *
     * @return  Type.ANY, or the Analysis dynamic representation of the AST
     *          type, if it is resolvable.
     */
    public Type graciousType(NQJElement e, NQJType type) {
        return graciousType(e, type, null);
    }
}
