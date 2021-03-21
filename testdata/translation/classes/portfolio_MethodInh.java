// Tests that method inheritance is translated correctly (types and effects).

int main() {
    printInt(new MethodInhClass2().f());
    return 0;
}

class MethodInhClass1 {
    int f() {
        return 64;
    }
}

class MethodInhClass2 extends MethodInhClass1 {

}
