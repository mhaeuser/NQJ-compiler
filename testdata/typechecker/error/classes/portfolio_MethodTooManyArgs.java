// Tests that method calls with too many arguments error.

int main() {
    new MethodTooManyArgsClass().f(0, 1);
    return 0;
}

class MethodTooManyArgsClass {
    int f(int a) {
        return 0;
    }
}
