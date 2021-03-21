// Tests that references from a superclass to a subclass succeed (includes field accesses and method calls).

int main() {
    ImplicitCastsClass2 a;
    a = new ImplicitCastsClass2();
    a.f();
    a.y.f();
    a.y = a;
    a.y.y = a;
    return 0;
}

class ImplicitCastsClass1 {
    ImplicitCastsClass2 y;

    int f() {
        return 0;
    }
}

class ImplicitCastsClass2 extends ImplicitCastsClass1 {

}
