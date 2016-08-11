package selleck.email.interest;

import java.util.ArrayList;
import java.util.List;


public class MatchInterestByAbstract extends AbstractMatchInterest{
	
	public MatchInterestByAbstract(){
		super();
	}
	
	/**
	 * 把要匹配的内容分成一个个单词List<String>
	 * 摘要不需要分词
	 */
	@Override
	List<String> splitWord(String word) {
//				List<String> words = new ArrayList<String>();
//				List<String> wordsList = new ArrayList<String>(); // 最终分好的word list
//				String[] tmpwords = word.replace("/font>", "").replaceAll(escapeChar," ").split(" ");
//				for(String tmp:tmpwords){
//					if(tmp == null)continue;
//					tmp = tmp.trim().toString();
//					if(!tmp.equals("") && !tmp.equals(" "))
//						words.add(tmp);
//				}
//						
//				tmpwords = null;
//				//将单独的数字与相邻的词组合
//				for (int i = 0; i < words.size(); i++) {
//					if (isNumeric(words.get(i))) {
//						if(i!=0)
//							wordsList.add(words.get(i-1)+words.get(i));
//						if(i!=words.size()-1)
//							wordsList.add(words.get(i)+words.get(i+1));
//					}else{
//						wordsList.add(words.get(i));
//					}
//				}
//				return wordsList;
			List<String> words = new ArrayList<String>();
			words.add(word);
			return words;
	}
	
	/**
	 * 找到wos文章的摘要里关键词的缩写。stem cells(SCs) 要记录SCs也作为关键词
	 */
	@Override
	List<String> findShortKeyword(String word) {
		
		return null;
	}
	
	
}
