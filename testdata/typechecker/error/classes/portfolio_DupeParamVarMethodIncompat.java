// Tests that duplicate and incompatible method parameters and fields error.

int main() {
    return 0;
}

class DupeParamVarMethodIncompatClass {
    int f(int x) {
        boolean x;
        return 0;
    }
}
