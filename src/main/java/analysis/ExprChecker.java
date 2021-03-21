package analysis;

import analysis.TypeContext.VarRef;
import notquitejava.ast.*;

/**
 * Matcher implementation for expressions returning a NQJ type.
 */
public class ExprChecker implements NQJExpr.Matcher<Type>, NQJExprL.Matcher<Type> {
    private final Analysis analysis;
    private final TypeContext ctxt;

    public ExprChecker(Analysis analysis, TypeContext ctxt) {
        this.analysis = analysis;
        this.ctxt = ctxt;
    }

    Type check(NQJExpr e) {
        return e.match(this);
    }

    Type check(NQJExprL e) {
        return e.match(this);
    }

    void expect(NQJExpr e, Type expected) {
        Type actual = check(e);
        if (!actual.isSubtypeOf(expected)) {
            analysis.addError(e, "Expected expression of type " + expected
                    + " but found " + actual + ".");
        }
    }

    Type expectArray(NQJExpr e) {
        Type actual = check(e);
        if (!(actual instanceof ArrayType)) {
            analysis.addError(e, "Expected expression of array type,  but found " + actual + ".");
            return Type.ANY;
        } else {
            return actual;
        }
    }

    @Override
    public Type case_ExprUnary(NQJExprUnary exprUnary) {
        Type t = check(exprUnary.getExpr());
        return exprUnary.getUnaryOperator().match(new NQJUnaryOperator.Matcher<Type>() {

            @Override
            public Type case_UnaryMinus(NQJUnaryMinus unaryMinus) {
                expect(exprUnary.getExpr(), Type.INT);
                return Type.INT;
            }

            @Override
            public Type case_Negate(NQJNegate negate) {
                expect(exprUnary.getExpr(), Type.BOOL);
                return Type.BOOL;
            }
        });
    }

    @Override
    public Type case_MethodCall(NQJMethodCall methodCall) {
        //
        // CHANGE: Implement method call type evaluation.
        //
        final NQJExpr receiver = methodCall.getReceiver();
        final String methodName = methodCall.getMethodName();
        final Type t = check(receiver);
        //
        // Type.INVALID would not follow the gracious name and type analysis
        // approach used throughout the architecture.
        //
        if (t == Type.INVALID) {
            throw new IllegalStateException();
        }
        //
        // Type.INVALID and Type.ANY must have output an error already,
        // graciously continue analysis.
        //
        if (t == Type.ANY) {
            return Type.ANY;
        }
        //
        // Only classes can have methods.
        //
        if (!(t instanceof ClassType)) {
            analysis.addError(methodCall, "Receiver is not a class object.");
            return Type.ANY;
        }
        //
        // Look up the return type of the method, and add errors appropriately.
        //
        final ClassType classType = (ClassType) t;
        final NQJFunctionDecl method = classType.getMethod(methodName);
        if (method == null) {
            analysis.addError(
                methodCall,
                "No such method " + methodName + " in class " + classType.toString()
            );
            return Type.ANY;
        }

        final NQJExprList args = methodCall.getArguments();
        final NQJVarDeclList params = method.getFormalParameters();
        checkCallArguments(methodCall, args, params);
        methodCall.setFunctionDeclaration(method);
        return analysis.graciousType(method.getReturnType());
    }


    @Override
    public Type case_ArrayLength(NQJArrayLength arrayLength) {
        expectArray(arrayLength.getArrayExpr());
        return Type.INT;
    }

    @Override
    public Type case_ExprThis(NQJExprThis exprThis) {
        //
        // CHANGE: Implement 'this' type evaluation.
        //
        final Type classType = this.ctxt.getThisType();
        if (classType == Type.ANY) {
            analysis.addError(exprThis, "Keyword 'this' is only valid in class methods.");
        }
        
        return classType;
    }

    @Override
    public Type case_ExprBinary(NQJExprBinary exprBinary) {
        return exprBinary.getOperator().match(new NQJOperator.Matcher<>() {
            @Override
            public Type case_And(NQJAnd and) {
                expect(exprBinary.getLeft(), Type.BOOL);
                expect(exprBinary.getRight(), Type.BOOL);
                return Type.BOOL;
            }

            @Override
            public Type case_Times(NQJTimes times) {
                return case_intOperation();
            }

            @Override
            public Type case_Div(NQJDiv div) {
                return case_intOperation();
            }

            @Override
            public Type case_Plus(NQJPlus plus) {
                return case_intOperation();
            }

            @Override
            public Type case_Minus(NQJMinus minus) {
                return case_intOperation();
            }

            private Type case_intOperation() {
                expect(exprBinary.getLeft(), Type.INT);
                expect(exprBinary.getRight(), Type.INT);
                return Type.INT;
            }

            @Override
            public Type case_Equals(NQJEquals equals) {
                Type l = check(exprBinary.getLeft());
                Type r = check(exprBinary.getRight());
                if (!l.isSubtypeOf(r) && !r.isSubtypeOf(l)) {
                    analysis.addError(exprBinary, "Cannot compare types " + l + " and " + r + ".");
                }
                return Type.BOOL;
            }

            @Override
            public Type case_Less(NQJLess less) {
                expect(exprBinary.getLeft(), Type.INT);
                expect(exprBinary.getRight(), Type.INT);
                return Type.BOOL;
            }
        });
    }

    @Override
    public Type case_ExprNull(NQJExprNull exprNull) {
        return Type.NULL;
    }

    /**
     * Verifies an AST function call for argument validity.
     * Errors are emitted appropriately.
     *
     * @param call    The AST function call to validate.
     * @param args    The AST argument list to validate.
     * @param params  The AST parameter list to validate against.
     */
    private void checkCallArguments(NQJElement call, NQJExprList args, NQJVarDeclList params) {
        if (args.size() < params.size()) {
            analysis.addError(call, "Not enough arguments.");
        } else if (args.size() > params.size()) {
            analysis.addError(call, "Too many arguments.");
        } else {
            for (int i = 0; i < params.size(); i++) {
                expect(args.get(i), analysis.graciousType(params.get(i).getType()));
            }
        }
    }

    @Override
    public Type case_FunctionCall(NQJFunctionCall functionCall) {
        NQJFunctionDecl m = analysis.getNameTable().lookupFunction(functionCall.getMethodName());
        if (m == null) {
            analysis.addError(functionCall, "Function " + functionCall.getMethodName()
                    + " does not exists.");
            return Type.ANY;
        }
        NQJExprList args = functionCall.getArguments();
        NQJVarDeclList params = m.getFormalParameters();
        //
        // CHANGE: Move argument validation to a method that is shared between
        //         method and function calls.
        //
        checkCallArguments(functionCall, args, params);
        functionCall.setFunctionDeclaration(m);
        return analysis.graciousType(m.getReturnType());
    }

    @Override
    public Type case_Number(NQJNumber number) {
        return Type.INT;
    }

    @Override
    public Type case_NewArray(NQJNewArray newArray) {
        expect(newArray.getArraySize(), Type.INT);
        //
        // CHANGE: Add unresolved type error-checking.
        //
        final NQJType arrayDecType = newArray.getBaseType();
        final Type arrayType = analysis.graciousType(newArray, arrayDecType);
        ArrayType t = new ArrayType(arrayType);
        newArray.setArrayType(t);
        return t;
    }

    @Override
    public Type case_NewObject(NQJNewObject newObject) {
        //
        // CHANGE: Implement object instantiation type evaluation.
        //
        final ClassType classType = analysis.getNameTable().getClassType(newObject.getClassName());
        if (classType == null) {
            this.analysis.addUnresolvedTypeError(newObject, newObject.getClassName());
            //
            // Return Type.ANY to allow for gracious name and type analysis.
            //
            return Type.ANY;
        }
        
        return classType;
    }

    @Override
    public Type case_BoolConst(NQJBoolConst boolConst) {
        return Type.BOOL;
    }

    @Override
    public Type case_Read(NQJRead read) {
        return read.getAddress().match(this);
    }

    @Override
    public Type case_FieldAccess(NQJFieldAccess fieldAccess) {
        //
        // CHANGE: Implement field access type evaluation.
        //
        final NQJExpr receiver = fieldAccess.getReceiver();
        final String fieldName = fieldAccess.getFieldName();
        final Type t = check(receiver);
        //
        // Type.INVALID would not follow the gracious name and type analysis
        // approach used throughout the architecture.
        //
        if (t == Type.INVALID) {
            throw new IllegalStateException();
        }
        //
        // Type.INVALID and Type.ANY must have output an error already,
        // graciously continue analysis.
        //
        if (t == Type.ANY) {
            return Type.ANY;
        }
        //
        // Only classes can have fields.
        //
        if (!(t instanceof ClassType)) {
            analysis.addError(receiver, "Receiver is not a class object.");
            return Type.ANY;
        }
        //
        // Look up the type of the field, and add errors appropriately.
        //
        final ClassType classType = (ClassType) t;
        final NQJVarDecl varDecl = classType.getField(fieldAccess.getFieldName());
        if (varDecl == null) {
            analysis.addError(
                receiver,
                "No such field " + fieldName + " in class " + classType.toString()
            );
            return Type.ANY;
        }
        
        return analysis.graciousType(varDecl.getType());
    }

    @Override
    public Type case_VarUse(NQJVarUse varUse) {
        VarRef ref = ctxt.lookupVar(varUse.getVarName());
        //
        // CHANGE: Variables shadow fields, so look up fields as a fallback.
        //
        if (ref == null) {
            ref = ctxt.lookupField(varUse.getVarName());
        }
        if (ref == null) {
            analysis.addError(varUse, "Variable " + varUse.getVarName() + " is not defined.");
            return Type.ANY;
        }
        varUse.setVariableDeclaration(ref.decl);
        return ref.type;
    }

    @Override
    public Type case_ArrayLookup(NQJArrayLookup arrayLookup) {
        Type type = analysis.checkExpr(ctxt, arrayLookup.getArrayExpr());
        expect(arrayLookup.getArrayIndex(), Type.INT);
        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            arrayLookup.setArrayType(arrayType);
            return arrayType.getBaseType();
        }
        analysis.addError(arrayLookup, "Expected an array for array-lookup, but found " + type);
        return Type.ANY;
    }
}
