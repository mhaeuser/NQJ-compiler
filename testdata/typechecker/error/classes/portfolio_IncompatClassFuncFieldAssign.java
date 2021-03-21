// Tests that an incompatible field assigns in a function errors.

int main() {
    return 0;
}

int f() {
    IncompatClassFuncFieldAssignClass1 a;
    a = new IncompatClassFuncFieldAssignClass1();
    a.a = new IncompatClassFuncFieldAssignClass2();
    return 0;
}

class IncompatClassFuncFieldAssignClass1 {
    IncompatClassFuncFieldAssignClass1 a;
}

class IncompatClassFuncFieldAssignClass2 {

}
