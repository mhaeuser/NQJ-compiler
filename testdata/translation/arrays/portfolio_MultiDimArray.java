// Tests that multi-dimensional arrays are translated correctly (allocation, read, write).

int main() {
    MultiDimArrayClass[][] a;
    a = new MultiDimArrayClass[3][];

    int i;
    i = 0;
    while (i < 3) {
        a[i] = new MultiDimArrayClass[9];
        int j;
        j = 0;
        while (j < 9) {
            a[i][j] = new MultiDimArrayClass();
            j = j + 1;
        }

        i = i + 1;
    }

    i = 0;
    while (i < 9) {
        a[1][i].x = i;
        i = i + 1;
    }

    i = 0;
    while (i < 9) {
        printInt(a[1][i].x);
        i = i + 1;
    }

    return 0;
}

class MultiDimArrayClass {
  int x;
}
