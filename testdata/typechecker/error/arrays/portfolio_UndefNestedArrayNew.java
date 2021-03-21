// Tests that nested array instantiations of an undefined class error.

int main() {
    (new UndefClass[9][])[0] = 0;
    return 0;
}
