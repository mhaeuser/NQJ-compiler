// Tests that inheritance cycles of length 2 with a 'root' outside the cycle error.

int main() {
    return 0;
}

class ClassCycle2ClassNonroot1 extends ClassCycle2ClassNonroot2 {

}

class ClassCycle2ClassNonroot2 extends ClassCycle2ClassNonroot3 {
  
}

class ClassCycle2ClassNonroot3 extends ClassCycle2ClassNonroot2 {
  
}
