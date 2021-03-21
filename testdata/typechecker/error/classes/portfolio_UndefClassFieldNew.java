// Tests that instantiations of undefined classes to fields in functions error.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

class UndefClassFieldClass {
    UndefClassFieldClass a;
}

int f(UndefClassFieldClass a) {
    a.a = new UndefClass();
}
