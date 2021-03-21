// Tests cross-references are implemented correctly (fields, methods, and inheritance).

int main() {
  new CrossRefClass1().b();
  return 0;
}

class CrossRefClass1 {
    int x;
    CrossRefClass2 refB;
    int b() {
        refB = new CrossRefClass2();
        this.rb(refB);
        return 0;
    }
    int rb(CrossRefClass2 b) {
        b.y = 1;
        if (x < 1) {
            b.c();
        }
        else
            x = x;
        printInt(b.y);
        return 0;
    }
}

class CrossRefClass2 extends CrossRefClass1 {
    int y;
    CrossRefClass3 refC;
    int c() {
        refC = new CrossRefClass3();
        this.rc(refC);
        return 0;
    }
    int rc(CrossRefClass3 c) {
        c.z = 1;
        c.a();
        printInt(c.z);
        return 0;
    }
}

class CrossRefClass3 extends CrossRefClass2 {
    int z;
    CrossRefClass1 refA;
    int a() {
        refA = new CrossRefClass1();
        this.ra(refA);
        return 0;
    }
    int ra(CrossRefClass1 a) {
        a.x = 1;
        a.b();
        printInt(a.x);
        this.x = 1;
        this.b();
        printInt(this.x);
        return 0;
    }
}
