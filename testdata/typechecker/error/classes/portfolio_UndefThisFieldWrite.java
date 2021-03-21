// Tests whether writes of undefined 'this'' fields error.

int main() {
    return 0;
}

class UndefFieldWriteClass {
    int f() {
        this.undefField = 0;
        return 0;
    }
}
