// Tests that duplicate and incompatible functions error.

int main() {
    return 0;
}

int f() {
    return 0;
}

int f(int x) {
    return 0;
}
