// Tests that field shadowing by incompatible parameters succeeds.

int main() {
    return 0;
}

class ParamShadowsFieldIncompatClass {
    boolean x;
    int f(int x) {
        x = 0;
        return 0;
    }
}
