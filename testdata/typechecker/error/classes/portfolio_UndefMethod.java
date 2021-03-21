// Tests that calls to undefined methods error.

int main() {
    new UndefFieldClass().undefMethod();
    return 0;
}

class UndefFieldClass {

}
