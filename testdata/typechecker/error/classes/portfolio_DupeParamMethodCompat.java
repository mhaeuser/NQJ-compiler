// Tests that duplicate but compatible method parameters error.

int main() {
    return 0;
}

class DupeParamMethodCompatClass {
    int f(int x, int x) {
        return 0;
    }
}
