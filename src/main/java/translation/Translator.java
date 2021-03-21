package translation;

import analysis.ArrayType;
import analysis.ClassType;
import minillvm.ast.*;
import notquitejava.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static frontend.AstPrinter.print;
import static minillvm.ast.Ast.*;


/**
 * Entry class for the translation phase.
 */
public class Translator {

    private final StmtTranslator stmtTranslator = new StmtTranslator(this);
    private final ExprLValue exprLValue = new ExprLValue(this);
    private final ExprRValue exprRValue = new ExprRValue(this);
    private final Map<NQJFunctionDecl, Proc> functionImpl = new HashMap<>();
    private final Prog prog = Prog(TypeStructList(), GlobalList(), ProcList());
    private final NQJProgram javaProg;
    private final Map<NQJVarDecl, TemporaryVar> localVarLocation = new HashMap<>();
    private final Map<analysis.Type, Type> translatedType = new HashMap<>();
    private final Map<Type, TypeStruct> arrayStruct = new HashMap<>();
    private final Map<Type, Proc> newArrayFuncForType = new HashMap<>();

    // mutable state
    private Proc currentProcedure;
    private BasicBlock currentBlock;

    /**
     * An index of class instance structures by class name.
     * Class names are guaranteed to be unique by Analysis.
     */
    private final Map<String, TypeStruct> classStruct = new HashMap<>();

    /**
     * An index of class instantiation functions by class struct name.
     * Class struct names are guaranteed to be unique by class translation.
     */
    private final Map<String, Proc> newClassFuncForType = new HashMap<>();

    /**
     * An index of 'this' types by AST method declaration.
     * AST method declarations are unique by design.
     */
    private final Map<NQJFunctionDecl, TypePointer> methodThisType = new HashMap<>();

    /**
     * An index of class Virtual Method Table structures by class struct name.
     * Class struct names are guaranteed to be unique by class translation.
     */
    private final Map<String, TypeStruct> classVTableStructs = new HashMap<>();

    /**
     * An index of class VTable global variables by class struct name.
     * Class struct names are guaranteed to be unique by class translation.
     */
    private final Map<String, Global> classVTableVars = new HashMap<>();

    public Translator(NQJProgram javaProg) {
        this.javaProg = javaProg;
    }

    /**
     * Translates given program into a mini llvm program.
     */
    public Prog translate() {
        //
        // Translate classes. This must happen before translating functions, so
        // that they can easily reference any class.
        //
        translateClasses();

        // translate functions except main
        // has only access to functions
        translateFunctions();

        // translate main function
        // has access to functions
        translateMainFunction();

        finishNewArrayProcs();

        return prog;
    }

    TemporaryVar getLocalVarLocation(NQJVarDecl varDecl) {
        return localVarLocation.get(varDecl);
    }

    private void finishNewArrayProcs() {
        for (Type type : newArrayFuncForType.keySet()) {
            finishNewArrayProc(type);
        }
    }

    private void finishNewArrayProc(Type componentType) {
        final Proc newArrayFunc = newArrayFuncForType.get(componentType);
        final Parameter size = newArrayFunc.getParameters().get(0);

        addProcedure(newArrayFunc);
        setCurrentProc(newArrayFunc);

        BasicBlock init = newBasicBlock("init");
        addBasicBlock(init);
        setCurrentBlock(init);
        TemporaryVar sizeLessThanZero = TemporaryVar("sizeLessThanZero");
        addInstruction(BinaryOperation(sizeLessThanZero,
                VarRef(size), Slt(), ConstInt(0)));
        BasicBlock negativeSize = newBasicBlock("negativeSize");
        BasicBlock goodSize = newBasicBlock("goodSize");
        currentBlock.add(Branch(VarRef(sizeLessThanZero), negativeSize, goodSize));

        addBasicBlock(negativeSize);
        negativeSize.add(HaltWithError("Array Size must be positive"));

        addBasicBlock(goodSize);
        setCurrentBlock(goodSize);

        // allocate space for the array

        TemporaryVar arraySizeInBytes = TemporaryVar("arraySizeInBytes");
        addInstruction(BinaryOperation(arraySizeInBytes,
                VarRef(size), Mul(), byteSize(componentType)));

        // 4 bytes for the length
        TemporaryVar arraySizeWithLen = TemporaryVar("arraySizeWitLen");
        addInstruction(BinaryOperation(arraySizeWithLen,
                VarRef(arraySizeInBytes), Add(), ConstInt(4)));

        TemporaryVar mallocResult = TemporaryVar("mallocRes");
        addInstruction(Alloc(mallocResult, VarRef(arraySizeWithLen)));
        //
        // CHANGE: Add handling for Out-Of-Memory scenarios.
        //
        VarRef mallocResultRes = VarRef(mallocResult);
        addNullcheck(mallocResultRes, "Out of memory exception");
        TemporaryVar newArray = TemporaryVar("newArray");
        addInstruction(Bitcast(newArray,
                getArrayPointerType(componentType), mallocResultRes));

        // store the size
        TemporaryVar sizeAddr = TemporaryVar("sizeAddr");
        addInstruction(GetElementPtr(sizeAddr,
                VarRef(newArray), OperandList(ConstInt(0), ConstInt(0))));
        addInstruction(Store(VarRef(sizeAddr), VarRef(size)));

        // initialize Array with zeros:
        final BasicBlock loopStart = newBasicBlock("loopStart");
        final BasicBlock loopBody = newBasicBlock("loopBody");
        final BasicBlock loopEnd = newBasicBlock("loopEnd");
        final TemporaryVar iVar = TemporaryVar("iVar");
        currentBlock.add(Alloca(iVar, TypeInt()));
        currentBlock.add(Store(VarRef(iVar), ConstInt(0)));
        currentBlock.add(Jump(loopStart));

        // loop condition: while i < size
        addBasicBlock(loopStart);
        setCurrentBlock(loopStart);
        final TemporaryVar i = TemporaryVar("i");
        final TemporaryVar nextI = TemporaryVar("nextI");
        loopStart.add(Load(i, VarRef(iVar)));
        TemporaryVar smallerSize = TemporaryVar("smallerSize");
        addInstruction(BinaryOperation(smallerSize,
                VarRef(i), Slt(), VarRef(size)));
        currentBlock.add(Branch(VarRef(smallerSize), loopBody, loopEnd));

        // loop body
        addBasicBlock(loopBody);
        setCurrentBlock(loopBody);
        // ar[i] = 0;
        final TemporaryVar iAddr = TemporaryVar("iAddr");
        addInstruction(GetElementPtr(iAddr,
                VarRef(newArray), OperandList(ConstInt(0), ConstInt(1), VarRef(i))));
        addInstruction(Store(VarRef(iAddr), defaultValue(componentType)));

        // nextI = i + 1;
        addInstruction(BinaryOperation(nextI, VarRef(i), Add(), ConstInt(1)));
        // store new value in i
        addInstruction(Store(VarRef(iVar), VarRef(nextI)));

        loopBody.add(Jump(loopStart));

        addBasicBlock(loopEnd);
        loopEnd.add(ReturnExpr(VarRef(newArray)));
    }

    private void translateFunctions() {
        for (NQJFunctionDecl functionDecl : javaProg.getFunctionDecls()) {
            if (functionDecl.getName().equals("main")) {
                continue;
            }
            initFunction(functionDecl);
        }
        for (NQJFunctionDecl functionDecl : javaProg.getFunctionDecls()) {
            if (functionDecl.getName().equals("main")) {
                continue;
            }
            //
            // CHANGE: Adapt to the new declaration. Parameters are not offset.
            //
            translateFunction(functionDecl, 0);
        }
    }

    private void translateMainFunction() {
        NQJFunctionDecl f = null;
        for (NQJFunctionDecl functionDecl : javaProg.getFunctionDecls()) {
            if (functionDecl.getName().equals("main")) {
                f = functionDecl;
                break;
            }
        }

        if (f == null) {
            throw new IllegalStateException("Main function expected");
        }

        Proc proc = Proc("main", TypeInt(), ParameterList(), BasicBlockList());
        addProcedure(proc);
        functionImpl.put(f, proc);

        setCurrentProc(proc);
        BasicBlock initBlock = newBasicBlock("init");
        addBasicBlock(initBlock);
        setCurrentBlock(initBlock);

        // allocate space for the local variables
        allocaLocalVars(f.getMethodBody());

        // translate
        translateStmt(f.getMethodBody());
    }

    private void initFunction(NQJFunctionDecl f) {
        Type returnType = translateType(f.getReturnType());
        ParameterList params = f.getFormalParameters()
                .stream()
                .map(p -> Parameter(translateType(p.getType()), p.getName()))
                .collect(Collectors.toCollection(Ast::ParameterList));
        Proc proc = Proc(f.getName(), returnType, params, BasicBlockList());
        addProcedure(proc);
        functionImpl.put(f, proc);
    }

    //
    // CHANGE: Document the function as it has been modified.
    //
    /**
     * Translates a function to a LLVM procedure.
     * A method is considered to be a special case of a function, with
     * hiddenParams=1 to account for the hidden 'this' parameter.
     *
     * @param m             The AST function declaration to translate.
     * @param hiddenParams  The amount of hidden parameters. Hidden parameters
     *                      preceed all non-hidden parameters.
     */
    private void translateFunction(NQJFunctionDecl m, int hiddenParams) {
        Proc proc = functionImpl.get(m);
        setCurrentProc(proc);
        BasicBlock initBlock = newBasicBlock("init");
        addBasicBlock(initBlock);
        setCurrentBlock(initBlock);

        localVarLocation.clear();

        // store copies of the parameters in Allocas, to make uniform read/write access possible

        //
        // CHANGE: The function parameters may be offset, e.g. by 1 for the
        //         'this' parameter when translating a method.
        //
        int i = hiddenParams;
        for (NQJVarDecl param : m.getFormalParameters()) {
            TemporaryVar v = TemporaryVar(param.getName());
            addInstruction(Alloca(v, translateType(param.getType())));
            //
            // Arguments are offset by +1 due to the 'this' parameter.
            //
            addInstruction(Store(VarRef(v), VarRef(proc.getParameters().get(i))));
            localVarLocation.put(param, v);
            i++;
        }

        // allocate space for the local variables
        allocaLocalVars(m.getMethodBody());

        translateStmt(m.getMethodBody());
    }

    void translateStmt(NQJStatement s) {
        addInstruction(CommentInstr(sourceLine(s) + " start statement : " + printFirstline(s)));
        s.match(stmtTranslator);
        addInstruction(CommentInstr(sourceLine(s) + " end statement: " + printFirstline(s)));
    }

    int sourceLine(NQJElement e) {
        while (e != null) {
            if (e.getSourcePosition() != null) {
                return e.getSourcePosition().getLine();
            }
            e = e.getParent();
        }
        return 0;
    }

    private String printFirstline(NQJStatement s) {
        String str = print(s);
        str = str.replaceAll("\n.*", "");
        return str;
    }

    BasicBlock newBasicBlock(String name) {
        BasicBlock block = BasicBlock();
        block.setName(name);
        return block;
    }

    void addBasicBlock(BasicBlock block) {
        currentProcedure.getBasicBlocks().add(block);
    }

    BasicBlock getCurrentBlock() {
        return currentBlock;
    }

    void setCurrentBlock(BasicBlock currentBlock) {
        this.currentBlock = currentBlock;
    }


    void addProcedure(Proc proc) {
        prog.getProcedures().add(proc);
    }

    void setCurrentProc(Proc currentProc) {
        if (currentProc == null) {
            throw new RuntimeException("Cannot set proc to null");
        }
        this.currentProcedure = currentProc;
    }

    private void allocaLocalVars(NQJBlock methodBody) {
        methodBody.accept(new NQJElement.DefaultVisitor() {
            @Override
            public void visit(NQJVarDecl localVar) {
                super.visit(localVar);
                TemporaryVar v = TemporaryVar(localVar.getName());
                addInstruction(Alloca(v, translateType(localVar.getType())));
                localVarLocation.put(localVar, v);
            }
        });
    }

    void addInstruction(Instruction instruction) {
        currentBlock.add(instruction);
    }

    Type translateType(NQJType type) {
        return translateType(type.getType());
    }

    Type translateType(analysis.Type t) {
        Type result = translatedType.get(t);
        if (result == null) {
            if (t == analysis.Type.INT) {
                result = TypeInt();
            } else if (t == analysis.Type.BOOL) {
                result = TypeBool();
            } else if (t instanceof ArrayType) {
                ArrayType at = (ArrayType) t;
                result = TypePointer(getArrayStruct(translateType(at.getBaseType())));
            } else if (t instanceof ClassType) {
                //
                // CHANGE: Add translation for class types.
                //
                final ClassType classType = (ClassType) t;
                result = TypePointer(getClassStruct(classType.getDecl().getName()));
            } else {
                throw new RuntimeException("unhandled case " + t);
            }
            translatedType.put(t, result);
        }
        return result;
    }

    Parameter getThisParameter() {
        // in our case 'this' is always the first parameter
        return currentProcedure.getParameters().get(0);
    }

    Operand exprLvalue(NQJExprL e) {
        return e.match(exprLValue);
    }

    Operand exprRvalue(NQJExpr e) {
        return e.match(exprRValue);
    }

    void addNullcheck(Operand arrayAddr, String errorMessage) {
        TemporaryVar isNull = TemporaryVar("isNull");
        addInstruction(BinaryOperation(isNull, arrayAddr.copy(), Eq(), Nullpointer()));

        BasicBlock whenIsNull = newBasicBlock("whenIsNull");
        BasicBlock notNull = newBasicBlock("notNull");
        currentBlock.add(Branch(VarRef(isNull), whenIsNull, notNull));

        addBasicBlock(whenIsNull);
        whenIsNull.add(HaltWithError(errorMessage));

        addBasicBlock(notNull);
        setCurrentBlock(notNull);
    }

    Operand getArrayLen(Operand arrayAddr) {
        TemporaryVar addr = TemporaryVar("length_addr");
        addInstruction(GetElementPtr(addr,
                arrayAddr.copy(), OperandList(ConstInt(0), ConstInt(0))));
        TemporaryVar len = TemporaryVar("len");
        addInstruction(Load(len, VarRef(addr)));
        return VarRef(len);
    }

    public Operand getNewArrayFunc(Type componentType) {
        Proc proc = newArrayFuncForType.computeIfAbsent(componentType, this::createNewArrayProc);
        return ProcedureRef(proc);
    }

    private Proc createNewArrayProc(Type componentType) {
        Parameter size = Parameter(TypeInt(), "size");
        return Proc("newArray",
                getArrayPointerType(componentType), ParameterList(size), BasicBlockList());
    }

    private Type getArrayPointerType(Type componentType) {
        return TypePointer(getArrayStruct(componentType));
    }

    TypeStruct getArrayStruct(Type type) {
        return arrayStruct.computeIfAbsent(type, t -> {
            TypeStruct struct = TypeStruct("array_" + type, StructFieldList(
                    StructField(TypeInt(), "length"),
                    StructField(TypeArray(type, 0), "data")
            ));
            prog.getStructTypes().add(struct);
            return struct;
        });
    }

    Operand addCastIfNecessary(Operand value, Type expectedType) {
        if (expectedType.equalsType(value.calculateType())) {
            return value;
        }
        TemporaryVar castValue = TemporaryVar("castValue");
        addInstruction(Bitcast(castValue, expectedType, value));
        return VarRef(castValue);
    }

    BasicBlock unreachableBlock() {
        return BasicBlock();
    }

    Type getCurrentReturnType() {
        return currentProcedure.getReturnType();
    }

    public Proc loadFunctionProc(NQJFunctionDecl functionDeclaration) {
        return functionImpl.get(functionDeclaration);
    }

    /**
     * return the number of bytes required by the given type.
     */
    public Operand byteSize(Type type) {
        return type.match(new Type.Matcher<>() {
            @Override
            public Operand case_TypeByte(TypeByte typeByte) {
                return ConstInt(1);
            }

            @Override
            public Operand case_TypeArray(TypeArray typeArray) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeProc(TypeProc typeProc) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeInt(TypeInt typeInt) {
                return ConstInt(4);
            }

            @Override
            public Operand case_TypeStruct(TypeStruct typeStruct) {
                return Sizeof(typeStruct);
            }

            @Override
            public Operand case_TypeNullpointer(TypeNullpointer typeNullpointer) {
                return ConstInt(8);
            }

            @Override
            public Operand case_TypeVoid(TypeVoid typeVoid) {
                return ConstInt(0);
            }

            @Override
            public Operand case_TypeBool(TypeBool typeBool) {
                return ConstInt(1);
            }

            @Override
            public Operand case_TypePointer(TypePointer typePointer) {
                return ConstInt(8);
            }
        });
    }

    private Operand defaultValue(Type componentType) {
        return componentType.match(new Type.Matcher<>() {
            @Override
            public Operand case_TypeByte(TypeByte typeByte) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeArray(TypeArray typeArray) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeProc(TypeProc typeProc) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeInt(TypeInt typeInt) {
                return ConstInt(0);
            }

            @Override
            public Operand case_TypeStruct(TypeStruct typeStruct) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeNullpointer(TypeNullpointer typeNullpointer) {
                return Nullpointer();
            }

            @Override
            public Operand case_TypeVoid(TypeVoid typeVoid) {
                throw new RuntimeException("TODO implement");
            }

            @Override
            public Operand case_TypeBool(TypeBool typeBool) {
                return ConstBool(false);
            }

            @Override
            public Operand case_TypePointer(TypePointer typePointer) {
                return Nullpointer();
            }
        });
    }

    /**
     * Creates and indexes a function to instantiate a new class object.
     *
     * @param classStruct  The class instance structure to instantiate.
     *
     * @return  A function to instantiate a new class object.
     */
    private Proc createNewClassFunc(TypeStruct classStruct) {
        //
        // Preserve the old translation context to later restore.
        //
        final Proc oldProc = currentProcedure;
        final BasicBlock oldBlock = currentBlock;
        //
        // Create new class instantiation function for the given type.
        //
        final Proc newClassFunc = Proc("new_" + classStruct.getName(),
            TypePointer(classStruct), ParameterList(), BasicBlockList());
        addProcedure(newClassFunc);
        setCurrentProc(newClassFunc);

        final BasicBlock init = newBasicBlock("init");
        addBasicBlock(init);
        setCurrentBlock(init);
        //
        // Allocate space for the new class instance.
        //
        final TemporaryVar mallocResult = TemporaryVar("mallocRes");
        final VarRef mallocResultRef = VarRef(mallocResult);
        //
        // Add instructions to allocate the class instance memory.
        //
        addInstruction(Alloc(mallocResult, Sizeof(classStruct)));
        addNullcheck(mallocResultRef, "Out of memory exception");
        final TemporaryVar newClass = TemporaryVar("newClass");
        addInstruction(Bitcast(newClass, TypePointer(classStruct), mallocResultRef));
        //
        // Store the class VTable pointer.
        //
        final TemporaryVar sizeAddr = TemporaryVar("vtable");
        addInstruction(
            GetElementPtr(sizeAddr, VarRef(newClass), OperandList(ConstInt(0), ConstInt(0)))
        );
        addInstruction(
            Store(VarRef(sizeAddr), GlobalRef(classVTableVars.get(classStruct.getName())))
        );
        //
        // Initialise all class fields with their default values.
        //
        for (int i = 1; i < classStruct.getFields().size(); ++i) {
            final StructField field = classStruct.getFields().get(i);
            final TemporaryVar fieldVar = TemporaryVar(field.getName());
            //
            // Add instructions to store the field default value.
            //
            addInstruction(
                GetElementPtr(fieldVar, VarRef(newClass), OperandList(ConstInt(0), ConstInt(i)))
            );
            addInstruction(Store(VarRef(fieldVar), defaultValue(field.getType())));
        }
        //
        // Add instruction to return the class instance pointer.
        //
        addInstruction(ReturnExpr(VarRef(newClass)));
        //
        // Restore the old translation context.
        //
        currentProcedure = oldProc;
        currentBlock = oldBlock;

        return newClassFunc;
    }

    /**
     * Initializes and indexes a method declaration.
     *
     * @param classStruct  The class instance structure the method belongs to.
     * @param methodDecl   The AST method declaration to index.
     */
    private void initMethod(TypeStruct classStruct, NQJFunctionDecl methodDecl) {
        //
        // Index the 'this' type for the current method.
        //
        methodThisType.put(methodDecl, TypePointer(classStruct));
        //
        // Create and index a LLVM procedure declaration for the current method.
        //
        final Type returnType = translateType(methodDecl.getReturnType());
        final ParameterList params = methodDecl.getFormalParameters()
            .stream()
            .map(p -> Parameter(translateType(p.getType()), p.getName()))
            .collect(Collectors.toCollection(Ast::ParameterList));
        //
        // Add the 'this' parameter implicit to class methods.
        //
        params.addFront(Parameter(TypePointer(classStruct), "this"));
        final Proc proc = Proc(methodDecl.getName(), returnType, params, BasicBlockList());
        addProcedure(proc);
        functionImpl.put(methodDecl, proc);
    }

    /**
     * Gets the pointer type of 'this' for an AST method declaration.
     *
     * @param methodDecl  The AST method declaration to inspect.
     *
     * @return  The pointer type of 'this' for the AST method declaration.
     */
    public TypePointer getMethodThisType(NQJFunctionDecl methodDecl) {
        final TypePointer thisType = methodThisType.get(methodDecl);
        //
        // All methods must have had their class type indexed previously.
        //
        if (thisType == null) {
            throw new IllegalStateException();
        }

        return thisType;
    }

    /**
     * Gets a class Virtual Method Table structure.
     *
     * @param classStruct  The class structure to get the VTable structure of.
     *
     * @return  The Virtual Method Table structure for the class.
     */
    public TypeStruct getVTableStruct(TypeStruct classStruct) {
        return classVTableStructs.get(classStruct.getName());
    }

    /**
     * Retrieves a function to instantiate a new class object.
     *
     * @param classStruct  The class instance structure to instantiate.
     *
     * @return  A function to instantiate a new class object.
     */
    public Operand getNewClassFunc(TypeStruct classStruct) {
        Proc proc = newClassFuncForType.get(classStruct.getName());
        if (proc == null) {
            //
            // If no such function was indexed yet, create one.
            //
            proc = createNewClassFunc(classStruct);
            newClassFuncForType.put(classStruct.getName(), proc);
        }

        return ProcedureRef(proc);
    }

    /**
     * Translate an AST class declaration to an LLVM structure, including the
     * Virtual Method Table. The results are cached to the appropriate indices.
     *
     * @param classDecl  The AST class declaration to translate.
     */
    private void constructClassStructs(NQJClassDecl classDecl, TypeStruct struct) {
        final NQJClassDecl superclass = classDecl.getDirectSuperClass();
        final TypeStruct superclassStruct;
        final StructFieldList vtableFields;
        final ConstList vtableFieldsData;
        //
        // Base the class instance and VTable structured on the superclass ones.
        // The lists are ordered, hence the class instance and VTable structures
        // of a superclass will be prefixes of those of a subclass.
        // In consequence, downcasts are supported.
        //
        if (superclass != null) {
            superclassStruct = classStruct.get(superclass.getName());

            final TypeStruct superclassVTableStruct = getVTableStruct(superclassStruct);
            final Global superclassVTableVar = classVTableVars.get(superclassStruct.getName());
            final Const superclassVTableConst =  superclassVTableVar.getInitialValue();
            final ConstStruct superclassVTableData = (ConstStruct) superclassVTableConst;

            vtableFields = superclassVTableStruct.getFields().copy();
            vtableFieldsData = superclassVTableData.getValues().copy();
        } else {
            superclassStruct = null;
            vtableFields = StructFieldList();
            vtableFieldsData = ConstList();
        }
        //
        // Both method and field list order in AST class declarations is
        // strictly defined depending on their order in the class declaration.
        // The struct construction follows this order for both lists during
        // construction. In other words, translating the same class multiple
        // times will yield compatible structures.
        //
        for (final NQJFunctionDecl funcDecl : classDecl.getMethods()) {
            final ProcedureRef procRef = ProcedureRef(loadFunctionProc(funcDecl));
            final StructField procField = StructField(
                procRef.calculateType(),
                funcDecl.getName()
                );
            //
            // Check whether the superclass provides a method by this name to
            // conditionally perform method overriding.
            //
            boolean found = false;
            for (int i = 0; i < vtableFields.size(); ++i) {
                final StructField superField = vtableFields.get(i);
                if (superField.getName().equals(funcDecl.getName())) {
                    //
                    // If the method is overriding, update the according Virtual
                    // Method Table entry with the new method signature (updated
                    // 'this' parameter type) and procedure address.
                    //
                    vtableFields.set(i, procField);
                    vtableFieldsData.set(i, procRef);
                    found = true;
                    break;
                }
            }
            //
            // If the method is not overriding, add a field for it to the
            // Virtual Method Table.
            //
            if (!found) {
                vtableFields.add(procField);
                vtableFieldsData.add(procRef);
            }
        }
        //
        // Index the class Virtual Method Table structure.
        //
        final TypeStruct vtableStruct = TypeStruct("vtable_" + classDecl.getName(), vtableFields);
        final ConstStruct vtableData = ConstStruct(vtableStruct, vtableFieldsData);
        prog.getStructTypes().add(vtableStruct);
        //
        // Create a global variable for the VTable. It is retrieved by the
        // function creating new class instances to initialise set its VTable.
        //
        final Global vtableVar = Global(
            vtableStruct,
            "_vtable_" + classDecl.getName(),
            true,
            vtableData
        );
        prog.getGlobals().add(vtableVar);
        classVTableVars.put(struct.getName(), vtableVar);
        //
        // Class instance structures are deterministic for each AST class
        // declaration as expalined above.
        //
        struct.getFields().add(StructField(TypePointer(vtableStruct), "_vtable"));
        if (superclassStruct != null) {
            //
            // Copy all fields except for the _vtable at index 0.
            //
            for (int i = 1; i < superclassStruct.getFields().size(); ++i) {
                struct.getFields().add(superclassStruct.getFields().get(i).copy());
            }
        }
        for (final NQJVarDecl fieldDecl : classDecl.getFields()) {
            struct.getFields().add(
                StructField(translateType(fieldDecl.getType()), fieldDecl.getName())
            );
        }
        //
        // Index the class instance structure.
        //
        classVTableStructs.put(struct.getName(), vtableStruct);
    }

    /**
     * Initialize a class instance structure reference.
     *
     * <p>The published class instance structure will be empty. This is useful
     * so that all class references can be retrieved between cross-referencing
     * classes, before they are constructed and translated. The current class
     * declaration and all of its superclass declarations will be queued into
     * classQueue in descending order (subtype relation) without duplicates.
     *
     * @param classDecl   The AST class declaration to translate.
     * @param classQueue  Queue to add the class to. Also adds all superclasses
     *                    in descending order (subtype relation)
     */
    private void initClass(NQJClassDecl classDecl, List<NQJClassDecl> classQueue) {
        //
        // Classes may be attempted to be initialized multiple times.
        // All superclasses are known to be queued for all queued classes, so we
        // can terminate the recursion early.
        //
        if (classQueue.contains(classDecl)) {
            return;
        }
        //
        // Expose the preliminary class instance structure reference. This is
        // important for classes that cross-reference each other, so that
        // initMethod() and translateClassStructs() can fetch the type
        // references. They do not require fully populated sturctures (VTable,
        // class fields).
        //
        final TypeStruct struct = TypeStruct("class_" + classDecl.getName(), StructFieldList());
        classStruct.put(classDecl.getName(), struct);
        prog.getStructTypes().add(struct);
        //
        // We must add superclasses to the queue before subclasses so that the
        // VTable and struct information of former is available to latter in
        // translateClassStructs().
        //
        final NQJClassDecl superclass = classDecl.getDirectSuperClass();
        if (superclass != null) {
            initClass(superclass, classQueue);
        }
        //
        // Queue the current class for construction and translation.
        //
        classQueue.add(classDecl);
    }

    /**
     * Construct an LLVM class instance structure from an AST class declaration.
     *
     * <p>An appropriate Virtual Method Table is constructed and indexed.
     * Method declarations are published so that they are available to
     * translateClass().
     *
     * @param classDecl  The AST class declaration to translate.
     * @param struct     The class instance structure to populate.
     */
    private void constructClass(NQJClassDecl classDecl, TypeStruct struct) {
        //
        // This must be done in the construction stage as class methods may
        // cross-reference.
        //
        for (final NQJFunctionDecl methodDecl : classDecl.getMethods()) {
            initMethod(struct, methodDecl);
        }
        //
        // Construct the class instance and Virtual Method Table structures.
        //
        constructClassStructs(classDecl, struct);
    }

    /**
     * Translates all methods of an AST class declaration to LLVM procedures.
     *
     * @param classDecl  The AST class declaration to translate.
     */
    private void translateClass(NQJClassDecl classDecl) {
        //
        // Translate all class methods.
        //
        for (final NQJFunctionDecl methodDecl : classDecl.getMethods()) {
            //
            // Arguments are offset by 1 due to the 'this' parameter.
            //
            translateFunction(methodDecl, 1);
        }
    }

    /**
     * Translate all classes of the NQJ program into LLVM structures, and
     * translate all of their methods into LLVM procedures. Populate the
     * appropriate indices for class instance structures, Virtual Method Tables,
     * and class methods appropriately.
     */
    private void translateClasses() {
        //
        // Initialize all class declarations. classQueue will be sorted such
        // all superclasses appear before all subclasses.
        //
        final List<NQJClassDecl> classQueue = new ArrayList<>();
        for (final NQJClassDecl classDecl : javaProg.getClassDecls()) {
            initClass(classDecl, classQueue);
        }
        //
        // Construct all classes in descending order (subtype relation).
        //
        for (final NQJClassDecl classDecl : classQueue) {
            constructClass(classDecl, classStruct.get(classDecl.getName()));
        }
        //
        // Translate all class declarations.
        //
        for (final NQJClassDecl classDecl : classQueue) {
            translateClass(classDecl);
        }
    }

    /**
     * Gets a class instance structure by class name.
     *
     * @param className  The name of the class to resolve.
     *
     * @return  The class instance structure for the appropriate class.
     */
    public TypeStruct getClassStruct(String className) {
        return classStruct.get(className);
    }

    /**
     * Gets the appropriate structure type from a pointer.
     *
     * @param receiver  The receiver object to get the structure type of.
     *
     * @return The structure type of the pointer data.
     */
    public TypeStruct getStructFromPointer(Operand receiver) {
        return (TypeStruct) ((TypePointer) receiver.calculateType()).getTo();
    }
}
