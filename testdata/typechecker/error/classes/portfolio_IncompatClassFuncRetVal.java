// Tests that method declarations with an incompatible return value error.

int main() {
    return 0;
}

IncompatMethodRetValClass1 f() {
    return new IncompatMethodRetValClass2();
}

class IncompatMethodRetValClass1 {

}

class IncompatMethodRetValClass2 {

}
