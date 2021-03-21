// Tests that inheritance cycles of length 3 error.

int main() {
    return 0;
}

class ClassCycle2Class1 extends ClassCycle2Class3 {

}

class ClassCycle2Class2 extends ClassCycle2Class1 {
  
}

class ClassCycle2Class3 extends ClassCycle2Class2 {
  
}
