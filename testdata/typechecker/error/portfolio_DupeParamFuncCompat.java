// Tests that duplicate but compatible function parameters error.

int main() {
    return 0;
}

class DupeParamFuncCompatClass {
    int f(int x, int x) {
        return 0;
    }
}
