// Tests that method calls with too few arguments error.

int main() {
    new MethodTooFewArgsClass().f();
    return 0;
}

class MethodTooFewArgsClass {
    int f(int a) {
        return 0;
    }
}
