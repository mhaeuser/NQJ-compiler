// Tests that duplicate and incompatible methods error.

int main() {
    return 0;
}

class DupeMethodNameIncompatClass {
    int f() {
        return 0;
    }

    int f(int x) {
        return 0;
    }
}
