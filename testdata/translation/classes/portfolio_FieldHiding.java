// Tests that field hiding is translated correctly (storage and types).

int main() {
    FieldHidingClass2 a;
    a = new FieldHidingClass2();
    a.p1();
    a.p2();
    a.f();
    a.p1();
    a.p2();
    a.g();
    a.p1();
    a.p2();
    return 0;
}

class FieldHidingClass1 {
    int x;

    int f() {
        this.x = -1;
        return 0;
    }

    int p1() {
        printInt(this.x);
        return 0;
    }
}

class FieldHidingClass2 extends FieldHidingClass1 {
    boolean x;

    int g() {
        this.x = true;
        return 0;
    }

    int p2() {
        if (x)
            printInt(1);
        else
            printInt(0);

        return 0;
    }
}
