// Tests that method overriding (if compatible) succeeds.

int main() {
    return 0;
}

class MethodOverridingClass1 {
    int f() {
        return 0;
    }
}

class MethodOverridingClass2 extends MethodOverridingClass1 {
    int f() {
        return 0;
    }
}
