// Tests that field shadowing by incompatible variables succeeds.

int main() {
    return 0;
}

class VarShadowsFieldIncompatClass {
    int x;
    int f() {
        boolean x;
        x = false;
        return 0;
    }
}
