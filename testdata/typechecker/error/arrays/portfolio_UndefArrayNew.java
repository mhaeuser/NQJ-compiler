// Tests that array instantiations of an undefined class error.

int main() {
    (new UndefClass[9])[0] = 1;
    return 0;
}
