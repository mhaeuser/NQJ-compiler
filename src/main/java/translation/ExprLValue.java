package translation;

import minillvm.ast.Ast;
import minillvm.ast.BasicBlock;
import minillvm.ast.Operand;
import minillvm.ast.StructField;
import minillvm.ast.StructFieldList;
import minillvm.ast.TemporaryVar;
import minillvm.ast.TypeStruct;
import notquitejava.ast.*;

import static minillvm.ast.Ast.*;

/**
 * Evaluate L values.
 */
public class ExprLValue implements NQJExprL.Matcher<Operand> {
    private final Translator tr;

    public ExprLValue(Translator translator) {
        this.tr = translator;
    }

    @Override
    public Operand case_ArrayLookup(NQJArrayLookup e) {
        Operand arrayAddr = tr.exprRvalue(e.getArrayExpr());
        tr.addNullcheck(arrayAddr, "Nullpointer exception in line " + tr.sourceLine(e));

        Operand index = tr.exprRvalue(e.getArrayIndex());

        Operand len = tr.getArrayLen(arrayAddr);
        TemporaryVar smallerZero = Ast.TemporaryVar("smallerZero");
        TemporaryVar lenMinusOne = Ast.TemporaryVar("lenMinusOne");
        TemporaryVar greaterEqualLen = Ast.TemporaryVar("greaterEqualLen");
        TemporaryVar outOfBoundsV = Ast.TemporaryVar("outOfBounds");
        final BasicBlock outOfBounds = tr.newBasicBlock("outOfBounds");
        final BasicBlock indexInRange = tr.newBasicBlock("indexInRange");


        // smallerZero = index < 0
        tr.addInstruction(BinaryOperation(smallerZero, index, Slt(), Ast.ConstInt(0)));
        // lenMinusOne = length - 1
        tr.addInstruction(BinaryOperation(lenMinusOne, len, Sub(), Ast.ConstInt(1)));
        // greaterEqualLen = lenMinusOne < index
        tr.addInstruction(BinaryOperation(greaterEqualLen,
                VarRef(lenMinusOne), Slt(), index.copy()));
        // outOfBoundsV = smallerZero || greaterEqualLen
        tr.addInstruction(BinaryOperation(outOfBoundsV,
                VarRef(smallerZero), Or(), VarRef(greaterEqualLen)));

        tr.getCurrentBlock().add(Ast.Branch(VarRef(outOfBoundsV), outOfBounds, indexInRange));

        tr.addBasicBlock(outOfBounds);
        outOfBounds.add(Ast.HaltWithError("Index out of bounds error in line " + tr.sourceLine(e)));

        tr.addBasicBlock(indexInRange);
        tr.setCurrentBlock(indexInRange);
        TemporaryVar indexAddr = Ast.TemporaryVar("indexAddr");
        tr.addInstruction(Ast.GetElementPtr(indexAddr, arrayAddr, Ast.OperandList(
                Ast.ConstInt(0),
                Ast.ConstInt(1),
                index.copy()
        )));
        return VarRef(indexAddr);
    }

    /**
     * Gets a reference to a field by name.
     *
     * @param receiver   A reference to the receiver object.
     * @param fieldName  The name of the field to reference.
     *
     * @return  A reference to the requested field.
     */
    private Operand getFieldOperand(Operand receiver, String fieldName) {
        //
        // Analysis ensured the receiver is a class type.
        //
        final TypeStruct classStruct = tr.getStructFromPointer(receiver);
        //
        // Exclude the VTable pointer at index 0 as it's an internal detail.
        //
        if (!classStruct.getFields().get(0).getName().equals("_vtable")) {
            throw new IllegalStateException();
        }
        //
        // Look up the field index in the class instance structure.
        //
        boolean found = false;
        int i;
        //
        // Subclass structs are prefixed by the superclass struct with their own
        // fields added, thus in case of variable hiding the same name can occur
        // twice. Search backwards to account for this.
        // i is the index of the field in the class structure after termination.
        //
        for (i = classStruct.getFields().size() - 1; i > 0; --i) {
            final StructField field = classStruct.getFields().get(i);
            if (field.getName().equals(fieldName)) {
                found = true;
                break;
            }
        }
        //
        // Unknown fields should have been caught by Analysis.
        //
        if (!found) {
            throw new IllegalStateException();
        }
        //
        // Get a reference to the accessed field.
        //
        final TemporaryVar fieldPtr = TemporaryVar("fieldPtr");
        tr.addInstruction(
            GetElementPtr(fieldPtr, receiver, OperandList(ConstInt(0), ConstInt(i)))
        );
        return VarRef(fieldPtr);
    }

    @Override
    public Operand case_FieldAccess(NQJFieldAccess e) {
        //
        // CHANGE: Implement field access translation.
        //
        final Operand receiverOperand = tr.exprRvalue(e.getReceiver());
        //
        // Class objects may be null.
        //
        tr.addNullcheck(receiverOperand, "Nullpointer exception in line " + tr.sourceLine(e));
        return getFieldOperand(receiverOperand, e.getFieldName());
    }

    @Override
    public Operand case_VarUse(NQJVarUse e) {
        final NQJVarDecl varDecl = e.getVariableDeclaration();
        final TemporaryVar localVar = tr.getLocalVarLocation(varDecl);
        //
        // CHANGE: As variables shadow fields, if a variable by the name cannot
        //         be found, query fields.
        //
        if (localVar != null) {
            // local TemporaryVar
            return VarRef(localVar);
        }

        return getFieldOperand(VarRef(tr.getThisParameter()), varDecl.getName());
    }

}
