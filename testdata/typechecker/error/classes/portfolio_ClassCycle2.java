// Tests that inheritance cycles of length 2 error.

int main() {
    return 0;
}

class ClassCycle2Class1 extends ClassCycle2Class2 {

}

class ClassCycle2Class2 extends ClassCycle2Class1 {
  
}
