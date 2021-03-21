// Tests that incompatible arguments in method calls error.

int main() {
    new IncompatMethodArgClass1().f(new IncompatMethodArgClass2());
    return 0;
}

class IncompatMethodArgClass1 {
    int f(IncompatMethodArgClass1 a) {
        return 0;
    }
}

class IncompatMethodArgClass2 {

}
