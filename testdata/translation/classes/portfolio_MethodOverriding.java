// Tests that method overriding is implemented correctly (types and effects).

int main() {
    MethodOverridingClass1 a;
    MethodOverridingClass2 b;
    a = new MethodOverridingClass1();
    b = new MethodOverridingClass2();
    printInt(a.f());
    printInt(a.g());
    printInt(b.f());
    printInt(b.g());
    // Test method overriding for downcast classes.
    a = b;
    printInt(a.f());
    printInt(a.g());
    return 0;
}

class MethodOverridingClass1 {
    int f() {
        return 0;
    }

    int g() {
        return 0;
    }
}

class MethodOverridingClass2 extends MethodOverridingClass1 {
    int f() {
        return 1;
    }
}
