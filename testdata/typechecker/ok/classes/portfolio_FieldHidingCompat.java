// Tests that field hiding of compatible types succeeds.

int main() {
    return 0;
}

class FieldHidingCompatClass1 {
    int x;
}

class FieldHidingCompatClass2 extends FieldHidingCompatClass1 {
    int x;
}
