// Tests that field shadowing by compatible parameters succeeds.

int main() {
    return 0;
}

class ParamShadowsFieldCompatClass {
    int x;
    int f(int x) {
        return 0;
    }
}
