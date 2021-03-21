// Tests that incompatible assigns to fields error.

int main() {
    return 0;
}

class IncompatClassFuncFieldClass1 {
    IncompatClassFuncFieldClass1 a;

    int f() {
        IncompatClassFuncFieldClass1 a;
        a = new IncompatClassFuncFieldClass1();
        a.a = new IncompatClassFuncFieldClass2();
        return 0;
    }
}

class IncompatClassFuncFieldClass2 {

}
