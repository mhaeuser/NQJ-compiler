// Tests that null array dereferences are translated correctly (exception).

int main() {
    int[] a;
    a = null;
    a[0] = 0;
    return 0;
}
