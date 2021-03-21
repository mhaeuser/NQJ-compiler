// Tests that incompatible function variable assignments error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

int f() {
    IncompatFuncVarAssignClass1 a;
    a = new IncompatFuncVarAssignClass2();
    return 0;
}

class IncompatFuncVarAssignClass1 {

}

class IncompatFuncVarAssignClass2 {

}
