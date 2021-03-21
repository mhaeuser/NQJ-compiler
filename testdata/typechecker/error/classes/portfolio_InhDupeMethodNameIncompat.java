// Tests that inheriting duplicate and incompatible methods errors.

int main() {
    return 0;
}

class InhDupeMethodNameIncompatClass1 {
    int f() {
        return 0;
    }
}

class InhDupeMethodNameIncompatClass2 extends InhDupeMethodNameIncompatClass1 {
    int f(int x) {
        return 0;
    }
}
