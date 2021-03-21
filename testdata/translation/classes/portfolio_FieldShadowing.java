// Tests that field shadowing is translated correctly (storage and types, variables and parameters).

int main() {
    FieldShadowingClass a;
    a = new FieldShadowingClass();
    a.f();
    a.g(0);
    return 0;
}

class FieldShadowingClass {
    int x;

    int f() {
        int x;
        printInt(this.x);
        x = 1;
        printInt(x);
        printInt(this.x);
        return 0;
    }

    int g(int x) {
        printInt(this.x);
        x = 1;
        printInt(x);
        printInt(this.x);
        return 0;
    }
}
