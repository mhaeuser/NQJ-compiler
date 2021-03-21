// Tests that field hiding of incompatible types succeeds (includes correct type evalutaion).

int main() {
    return 0;
}

class FieldHidingClass1 {
    int x;
    int f() {
        x = 0;
        return 0;
    }
}

class FieldHidingClass2 extends FieldHidingClass1 {
    boolean x;
    int g() {
        x = false;
        return 0;
    }
}

int h(FieldHidingClass1 a) {
    a.x = 0;
    return 0;
}

int i(FieldHidingClass2 a) {
    a.x = false;
    return 0;
}
