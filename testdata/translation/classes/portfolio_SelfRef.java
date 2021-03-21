// Tests that self-references are translated correctly.

int main() {
    new SelfRefClass().f();
    return 0;
}

class SelfRefClass {
    SelfRefClass x;
    SelfRefClass[] y;
    int z;
    
    int f() {
        this.z = 9;
        x = new SelfRefClass();
        printInt(this.z);
        printInt(x.z);
        y = new SelfRefClass[4];
        return 0;
    }
}