// Tests that incompatible function parameter assigns error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

int f(IncompatFuncParamAssignClass1 a) {
    a = new IncompatFuncParamAssignClass2();
    return 0;
}

class IncompatFuncParamAssignClass1 {

}

class IncompatFuncParamAssignClass2 {

}
