// Tests that field inheritance is translated correctly (storage and types).

int main() {
    printInt(new FieldInhClass2().g());
    return 0;
}

class FieldInhClass1 {
    int x;

    int f() {
        x = 64;
        return 0;
    }
}

class FieldInhClass2 extends FieldInhClass1 {
    int g() {
      return x;
    }
}
