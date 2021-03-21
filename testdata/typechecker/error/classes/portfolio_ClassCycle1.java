// Tests that inheritance cycles of length 1 error.

int main() {
    return 0;
}

class ClassCycle1Class extends ClassCycle1Class {

}
