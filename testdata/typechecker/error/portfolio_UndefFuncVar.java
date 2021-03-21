// Tests that declarations of undefined classes in functions error.
// Because methods use the same code path, this test covers them too.

int main() {
    UndefClass a;
    return 0;
}
