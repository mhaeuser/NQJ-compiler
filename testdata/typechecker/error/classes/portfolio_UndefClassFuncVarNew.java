// Tests that instantiation of undefined classes to variables errors.

int main() {
    UndefClassFuncVarNewClass a;
    a = new UndefClass();
    return 0;
}

class UndefClassFuncVarNewClass {

}
