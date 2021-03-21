// Tests that incompatible arguments in function calls error.

int main() {
    f(false);
    return 0;
}

int f(int x) {
    return 0;
}
