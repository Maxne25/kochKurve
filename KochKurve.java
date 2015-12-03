public class Kochkurve {
	static void kochKurve(Staffelei s, int ordnung) {
	}

	public static void main(String[] args) {
		Staffelei s = new Staffelei();
		kochKurve(s, 3);
		s.rotate(120);
		kochKurve(s, 3);
		s.rotate(120);
		kochKurve(s, 3);
	}
}
