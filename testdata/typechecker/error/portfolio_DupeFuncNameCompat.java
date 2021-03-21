// Tests that duplicate but compatible functions error.

int main() {
    return 0;
}

int f() {
    return 0;
}

int f() {
    return 0;
}
