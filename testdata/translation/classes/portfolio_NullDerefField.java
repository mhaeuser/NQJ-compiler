// Tests that null field accesses are translated correctly (exception).

int main() {
    NullDerefClass a;
    a = null;
    a.x = 0;
    return 0;
}

class NullDerefClass {
    int x;
}
