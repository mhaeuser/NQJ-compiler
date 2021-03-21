// Tests that function declarations with an undefined class as return type error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

UndefClass f() {
    return 0;
}
