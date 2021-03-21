// Tests that implicit downcasts of compatible classes succeed.

int main() {
    ImplicitCastsClass2 a;
    a = new ImplicitCastsClass2();
    ImplicitCastsClass1 b;
    b = a;
    a.f(a);
    a.g(a);
    b = a.h();
    return 0;
}

class ImplicitCastsClass1 {
    ImplicitCastsClass1 x;
    ImplicitCastsClass2 y;

    int f(ImplicitCastsClass2 a) {
        this.x = a;
        return 0;
    }

    int g(ImplicitCastsClass1 a) {
        a = this.y;
        return 0;
    }
}

class ImplicitCastsClass2 extends ImplicitCastsClass1 {
    ImplicitCastsClass1 h() {
        return this;
    }
}
