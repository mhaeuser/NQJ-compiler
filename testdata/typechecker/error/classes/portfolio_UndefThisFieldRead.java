// Tests whether reads of undefined 'this'' fields error.

int main() {
    return 0;
}

class UndefFieldReadClass {
    int f() {
        int a;
        a = this.undefField;
        return 0;
    }
}
