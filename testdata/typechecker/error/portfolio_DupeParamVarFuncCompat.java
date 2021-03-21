// Tests that duplicate but compatible function parameters and variables error.

int main() {
    return 0;
}

int f(int x) {
    int x;
    return 0;
}
