// Tests that null method calls are translated correctly (exception).

int main() {
    NullDerefClass a;
    a = null;
    a.f();
    return 0;
}

class NullDerefClass {
    int f() {
        return 0;
    }
}
