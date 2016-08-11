package selleck.email.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.interest.MatchKeywordOrAbstractTest;
import selleck.email.interest.beans.DictClass;
import selleck.email.pojo.WOS;
import selleck.email.service.IDictService;
import selleck.email.service.IWOSService;
import selleck.email.service.impl.DictServiceImpl;
import selleck.email.service.impl.WOSServiceImpl;
import selleck.utils.Constants;

public class FindShortKeyWord {
	public static List<DictClass> dictMap = null;
	private static IWOSService wosService = new WOSServiceImpl(Constants.LIFE_SCIENCE_DB);
	private static IDictService dictService = new DictServiceImpl();
	public static Set<String> keywordSet = new HashSet<String>();
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		dictMap = getDict();
		System.out.println("dictMap size: "+dictMap.size());
		
		Criteria criteria = new Criteria();
		criteria.setOracleStart(200000);
		criteria.setOracleEnd(1000);
		List<WOS> wosList = wosService.selectByExample(criteria);
		System.out.println("wosList size: "+wosList.size());
		
		for(WOS wos : wosList){
			String abs = wos.getAbs();
			for(DictClass dict : dictMap){
				String dicKey = dict.getKeyword();
				// Pattern p = Pattern.compile("(\\G|[^a-zA-Z_0-9])"+dicKey+"([^a-zA-Z_0-9]|\\z)",Pattern.CASE_INSENSITIVE);
				Pattern p = Pattern.compile("(\\G|[^a-zA-Z_0-9])"+dicKey+"([ \\-]+\\w*){0,3}[ \\-]*\\(\\w{2,}\\)",Pattern.CASE_INSENSITIVE);
				// 去除标点  CYP3A4:  CYP3A4. Additionally, N-desethylamiodarone (DEA)
				//String regex ="\\([A-Za-z0-9]{2,}\\)";
				//Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
				Matcher matcher = p.matcher(abs);
				Set<String> assumedKeyWords = new HashSet<String>(); // 可能是缩写的，需要人工判断
				Set<String> definiteKeyWords = new HashSet<String>(); // 一定是缩写的，直接当关键词来搜索
				while(matcher.find()){
					System.out.println(dicKey+": "+matcher.group());
					String shortKeyWord = matcher.group(); // 形如:  aaa bbb ccc (ABC)
					if(shortKeyWord.startsWith(")") || shortKeyWord.startsWith("(")){
						shortKeyWord = shortKeyWord.substring(1);
					}
					String shortWord = shortKeyWord.substring(shortKeyWord.indexOf("(") + 1, shortKeyWord.indexOf(")")); // 缩写，形如: ABC
					
					// 如果缩写全是数字，不算做缩写
					Pattern pattern = Pattern.compile("[0-9]*");
					if(pattern.matcher(shortWord).matches()){
						continue;
					}
					
					// 如果缩写已经是关键字，不算做缩写
					int dictSize = dictMap.size();
					boolean d = false;
					for(int i =0;i<dictSize;i++){
						if(MatchKeywordOrAbstractTest.DICT_LIST.get(i).getKeyword().toLowerCase().equals(shortWord.toLowerCase())){
							d = true;
							System.out.println("===== keyword is shortword =======");
							break;
						}
					}
					if(d){
						continue;
					}
					
					String longWord = shortKeyWord.substring(0, shortKeyWord.indexOf("(")).trim(); // 全称，形如:  aaa bbb ccc
					String[] longWords = longWord.split("[ \\-]+");
					boolean b = true; // 是否肯定是关键词的缩写
					if(longWords.length ==  shortWord.length()){
						for(int i = 0;i < longWords.length;i++){
							if(!longWords[i].isEmpty() && !longWords[i].substring(0,1).equalsIgnoreCase(shortWord.substring(i, i+1)) ){
								b = false;
								break;
							}
						}
					}else{
						b = false;
					}
					if(b){
						definiteKeyWords.add(shortWord);
					}else{
						assumedKeyWords.add(matcher.group());
					}
					
					System.out.println("assumedKeyWords: "+assumedKeyWords);
					System.out.println("definiteKeyWords: "+definiteKeyWords);
					System.out.println();
				}
			}
		}
	}
	
	private static List<DictClass> getDict(){
		Criteria criteria = new Criteria();
		List<DictClass> dictList = dictService.selectByExample(criteria);
		return dictList;
	}

}
