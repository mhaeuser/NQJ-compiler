// Tests that duplicate but compatible method parameters and fields error.

int main() {
    return 0;
}

class DupeParamVarMethodCompatClass {
    int f(int x) {
        int x;
        return 0;
    }
}
