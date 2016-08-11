package selleck.test;

public class GeneralTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "http://www.vcahospitals.com/arden/our-team/administration";
		String link = "member";
		String relativePath = path.substring(0, 1);
		System.out.println(relativePath);
	}

}
