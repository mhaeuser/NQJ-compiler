// Tests that field shadowing by compatible variables succeeds.

int main() {
    return 0;
}

class VarShadowsFieldCompatClass {
    int x;
    int f() {
        int x;
        return 0;
    }
}
