// Tests that incompatible return values in functions error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

IncompatFuncRetValClass1 f() {
    return new IncompatFuncRetValClass2();
}

class IncompatFuncRetValClass1 {

}

class IncompatFuncRetValClass2 {

}
