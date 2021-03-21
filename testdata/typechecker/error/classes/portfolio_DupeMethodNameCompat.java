// Tests that duplicate but compatible methods error.

int main() {
    return 0;
}

class DupeMethodNameCompatClass {
    int f() {
        return 0;
    }

    int f() {
        return 0;
    }
}
