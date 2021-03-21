// Tests that incompatible variable assigns in functions error.

int main() {
    return 0;
}

int f(IncompatClassFuncParamAssignClass1 a) {
    a = new IncompatClassFuncParamAssignClass2();
    return 0;
}

class IncompatClassFuncParamAssignClass1 {

}

class IncompatClassFuncParamAssignClass2 {

}
