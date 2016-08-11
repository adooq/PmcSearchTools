package selleck.email.interest;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author fscai
 * 
 */
public class MatchInterestByKeyword extends AbstractMatchInterest {
	public MatchInterestByKeyword() {
		super();
		
		escapeChar = "\\(|\\)|\\{|\\}|\\[|\\]|,|:|\\.|\\*|;";
	}

	@Override
	List<String> splitWord(String keywords) {
		// 将标题分成每个单词
		List<String> words = new ArrayList<String>();
		List<String> wordsList = new ArrayList<String>(); // 最终分好的word list
		String[] tmpwords = keywords.replace("/font>", "").split(";");
		for (String tmp : tmpwords) {
			if (tmp == null)
				continue;
			tmp = tmp.replaceAll(escapeChar, " ").trim();
			tmp = tmp.replaceAll("\\s+", " "); // 把单词间有多个空格并成一个空格
			if (!tmp.equals("") && !tmp.equals(" "))
				words.add(tmp);
		}
		tmpwords = null;
		// 将单独的数字与相邻的词组合
		for (int i = 0; i < words.size(); i++) {
			if (isNumeric(words.get(i))) {
				if (i != 0)
					wordsList.add(words.get(i - 1) + words.get(i));
				if (i != words.size() - 1)
					wordsList.add(words.get(i) + words.get(i + 1));
			} else {
				wordsList.add(words.get(i));
			}
		}
		return wordsList;
	}

	@Override
	List<String> findShortKeyword(String word) {
		// TODO Auto-generated method stub
		return null;
	}

}
