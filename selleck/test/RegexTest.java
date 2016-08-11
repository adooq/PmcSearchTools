package selleck.test;

import org.jsoup.Jsoup;

public class RegexTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	String string="<asda>sadas</ahsdjklja>";
	string=Jsoup.parse(string).text();
	System.out.println(string);
	}

}
