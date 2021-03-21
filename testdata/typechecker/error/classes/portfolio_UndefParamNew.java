// Tests that instantiation of undefined classes to parameters in functions errors.
// Because methods use the same code path, this test covers them too.

int main() {
    return 0;
}

int f(UndefParamNewClass a) {
    a = new UndefClass();
}

class UndefParamNewClass {

}
