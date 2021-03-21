int main() {
		C[] arr;
		arr = new C[4];
		arr[0] = new C();
		arr[1] = arr[0];
		arr[2] = new C();
		arr[3] = arr[1];
		printInt(arr[0].get());
		printInt(arr[1].get());
		printInt(arr[2].get());
		printInt(arr[3].get());
		printInt(arr.length);
		printInt(f());
		return 0;
}

int f() {
	return 20;
}

class C {
	int x;

	int get() {
		this.x = this.x + 1;
		return x;
	}
}
