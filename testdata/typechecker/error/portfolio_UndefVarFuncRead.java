// Tests that reads of undefined variables in functions error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

int f() {
    undefVar = 0;
    return 0;
}
