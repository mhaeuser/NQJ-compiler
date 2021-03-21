// Tests that an incompatible variable assigns in a function errors.

int main() {
    return 0;
}

int f() {
    IncompatClassFuncVarAssignClass1 a;
    a = new IncompatClassFuncVarAssignClass2();
    return 0;
}

class IncompatClassFuncVarAssignClass1 {

}

class IncompatClassFuncVarAssignClass2 {

}
