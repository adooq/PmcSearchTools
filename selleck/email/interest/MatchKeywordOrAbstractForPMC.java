package selleck.email.interest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.frame.CreateFrame;
import common.handle.model.Criteria;
import selleck.email.interest.beans.DictClass;
import selleck.email.interest.beans.MatchedInterests;
import selleck.email.interest.beans.ProductClass;
import selleck.email.interest.beans.RelClass;
import selleck.email.pojo.Article;
import selleck.email.pojo.Author;
import selleck.email.pojo.AuthorInterest;
import selleck.email.service.IArticleService;
import selleck.email.service.IAuthorInterestService;
import selleck.email.service.IAuthorService;
import selleck.email.service.IDictService;
import selleck.email.service.IProductService;
import selleck.email.service.impl.ArticleServiceImpl;
import selleck.email.service.impl.AuthorInterestServiceImpl;
import selleck.email.service.impl.AuthorServiceImpl;
import selleck.email.service.impl.DictServiceImpl;
import selleck.email.service.impl.ProductServiceImpl;
import selleck.email.update.tools.JTextArea4Log;

/**
 * 为PMC的文章定兴趣
 * @author fscai
 *
 */
public class MatchKeywordOrAbstractForPMC {
	public static final  List<DictClass> DICT_LIST = getDict();
	public static final List<RelClass> productCategoryRel = getProductCategoryRel(); // 缓存产品和小靶点关系表
	public static final List<RelClass> smallBigRel = getSmallBigRel(); // 缓存小靶点和大靶点关系表
	public static final JTextArea4Log textArea = CreateFrame.getFrame().getLoggerTA();
	public static void main(String[] args) {
		findThreeInterestForArticle();
		
	}

	/**
	 * 按兴趣点关键词表，搜索文章的关键词,并总结出文章的产品、小靶点、大靶点的兴趣点，更新文章表
	 */
	public static void findThreeInterestForArticle() {
		MatchInterestByAbstract match = new MatchInterestByAbstract();
		List<MatchedInterests> matchedInterests = null;
		// String keyInteIdFlag = null;
		String newAbs = null; // 缩写替换成全称后的摘要
		String newTitle = null; // 缩写替换成全称后的title
		List<Article> articleList = new ArrayList<Article>();
		IArticleService articleService = new ArticleServiceImpl();
		int count = 3444798;
		int step = 1000;
		Criteria criteria = new Criteria();
		// while (count < 1400215) { // search_wos_by_publication max(id) 
		while (count < 3691083) { // selleck_edm_article max(id)  3691083
		// while (count <= 200) {
			// criteria.setOracleStart(34);
			// criteria.setOracleEnd(1);
			criteria.setWhereClause(" id >= "+count + " and id < "+(count+step));
			count += step;
			articleList = articleService.selectByExample(criteria);
			System.out.println("articleList size: "+articleList.size());
			if (articleList.size() == 0) {
				continue;
			}
			for (Iterator<Article> iter = articleList.iterator(); iter.hasNext();) {
				try {
					Article article = iter.next();
					if(article.getScore() != 0 || article.getParsed() ==1 ){
						continue;
					}
					newAbs = article.getAbs();
					newTitle = article.getTitle();
					String definiteKeyWord = article.getDefiniteKeyWords(); // aaa bbb ccc,ABC|aaa bbb ccc,ABC|aaa bbb ccc,ABC
					String assumedKeyWord = article.getAssumedKeyWords(); // aaa bbb ccc,ABC|aaa bbb ccc,ABC|aaa bbb ccc,ABC
					if ((assumedKeyWord == null || assumedKeyWord.isEmpty()) && (definiteKeyWord == null || definiteKeyWord.isEmpty())) {
						findShortKeyWord(newAbs + " " + newTitle, article , articleService);
					}
					definiteKeyWord = article.getDefiniteKeyWords();
					assumedKeyWord = article.getAssumedKeyWords();
					// 把标题和摘要里的缩写替换成全称
					// 目前暂时把待定关键字缩写assumedKeyWord也当成是确认的关键字
					if ((assumedKeyWord != null && !assumedKeyWord.isEmpty()) || (definiteKeyWord != null && !definiteKeyWord.isEmpty())) {	
						String dk = (definiteKeyWord != null ? definiteKeyWord : "") + (assumedKeyWord != null ? assumedKeyWord : "");
						// String dk = definiteKeyWord;
						String[] definiteKeyWords = dk.split("\\|");
						for (String dkw : definiteKeyWords) {
							if (dkw.trim().isEmpty()) {
								continue;
							}
							String longWord = dkw.split(",")[0];
							String shortWord = dkw.split(",")[1];
							newTitle = newTitle.replaceFirst("\\b" + shortWord + "(s\\b|\\b)", "") // 去除第一次出现缩写的地方，一般这个地方全称和缩写会同时出现，去除一次计数
									.replaceAll("\\b" + shortWord + "(s\\b|\\b)"," " + longWord + " ");

							newAbs = newAbs.replaceFirst("\\b" + shortWord + "(s\\b|\\b)", "") // 去除第一次出现缩写的地方，一般这个地方全称和缩写会同时出现，去除一次计数
									.replaceAll("\\b" + shortWord + "(s\\b|\\b)"," " + longWord + " ");
						}
					}

					boolean inTitle = true; // 是否在title中找到关键词
					matchedInterests = match.matchWordWithDict(newTitle);
					if (matchedInterests == null || matchedInterests.isEmpty()) {
						inTitle = false;
						matchedInterests = match.matchWordWithDict(newAbs);
					}
					// System.out.println("inTitle: " + inTitle);

					// 阈值过滤，去掉分数过低的文章。小靶点至少出现一次，大靶点至少出现4次，总分大于300分。
					int smallCount = 0;
					int bigCount = 0;
					int totalScore = 0;
					for (MatchedInterests o : matchedInterests) {
						o.setScore(o.getScore() * (inTitle ? 6 : 1)); // 标题的打分*6，因为关键词出现数量少但重要度高。
						totalScore += o.getScore();
						if (o.getProductId() != 0) {
							//
						} else if (o.getSmallId() != 0) {
							smallCount += o.getCount();
						} else if (o.getBigId() != 0) {
							bigCount += o.getCount();
						}
					}
					/* 暂时先不做阈值过滤，都算通过
					if (smallCount < 1 || bigCount < 4 || totalScore < 300) {
						article.setPassed(new Byte("0"));
					} else {
						article.setPassed(new Byte("1"));
					}
					*/
					article.setPassed(new Byte("1"));
					// System.out.println("score: " + totalScore);
					
					// 计算时间加分，取2008.1作为参照点，每晚一个月加10分。
					GregorianCalendar gc2008 = new GregorianCalendar(2008,Calendar.JANUARY, 1);
					GregorianCalendar gc = null;
					if(article.getpDate() != null && !article.getpDate().isEmpty()){
						gc = parseDate(article.getpDate()); // 出版年形如：'JAN 3 2013'  有可能没有月或日
					}else{
						gc = new GregorianCalendar(2011,Calendar.JANUARY, 1); // 有些pdate没有，取平均时间2011
					}
					int c = (gc.get(Calendar.YEAR) - gc2008.get(Calendar.YEAR))* 12 + gc.get(Calendar.MONTH) - gc2008.get(Calendar.MONTH);
					totalScore += c * 10;
					
					article.setScore(totalScore);

					String[] threeInterest = calCategoryInterest(matchedInterests, match); // String[] 产品兴趣点 小靶点兴趣点 大靶点兴趣点
					article.setProduct(threeInterest[0]);
					article.setSmall(threeInterest[1]);
					article.setBig(threeInterest[2]);

					articleService.updateArticle(article);
					
					textArea.append("parsed:"+article.getId()+"\n");
					// System.out.println("parsed:"+article.getId());
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}
	
	public static void findInterestForAuthor(){
		IAuthorService authroService = new AuthorServiceImpl();
		IArticleService articleService = new ArticleServiceImpl();
		IAuthorInterestService aiService = new AuthorInterestServiceImpl();
		int count = 0;
		Criteria criteria = new Criteria();
		criteria.setOracleStart(count);
		criteria.setOracleEnd(1000);
		while (true) {
			List<Author> authorList = authroService.selectByExample(criteria);
			if(authorList.size() ==0){
				break;
			}
			count += authorList.size();
			for(Author author : authorList){
				List<Article> articles = articleService.findArticleByAuthor(author);
				// 取总分数最高的两篇文章的三个兴趣点作为这个作者的六个兴趣点。
				Collections.sort(articles); // 排序从小到大，是不是要reverse?
				Article article1 = articles.get(0);
				AuthorInterest authorInterest1 = new AuthorInterest();
				authorInterest1.setAuthorId(author.getId());
				authorInterest1.setBig(article1.getBig());
				authorInterest1.setProduct(article1.getProduct());
				authorInterest1.setScore(article1.getScore());
				authorInterest1.setSmall(article1.getSmall());
				Article article2 = articles.get(1);
				AuthorInterest authorInterest2 = new AuthorInterest();
				authorInterest2.setAuthorId(author.getId());
				authorInterest2.setBig(article2.getBig());
				authorInterest2.setProduct(article2.getProduct());
				authorInterest2.setScore(article2.getScore());
				authorInterest2.setSmall(article2.getSmall());
				
				aiService.saveAuthorInterest(authorInterest1, authorInterest2);
			}
		}
		
	}

	/**
	 * 目的:如果没有较大靶点的兴趣，通过较小靶点的关联性来推测。这样，找到一篇文章中关于产品、小靶点、大靶点三个类别的兴趣点。
	 * 先找到matchList中的各个兴趣点对应的产品、小靶点、大靶点,如果有产品兴趣点，按兴趣点得分最高的作为产品类兴趣点，如果没有什么都不做。
	 * 如果有小靶点兴趣点，按兴趣点得分最高的作为小靶点类兴趣点；如果没有，查找所有产品兴趣点，总结一个相关度得分最高的小靶点作为小靶点类兴趣点；如果产品兴趣点也没有，什么都不做。
	 * 如果有大靶点兴趣点，按兴趣点得分最高的作为大靶点类兴趣点；如果没有，查找所有小靶点兴趣点，总结一个相关度得分最高的大靶点作为大靶点类兴趣点；如果小靶点兴趣点也没有,再去产品兴趣点中找。
	 * @param matchList
	 * @return String[] 代表一篇文章的产品、小靶点、大靶点三个类别的兴趣点
	 */
	/**
	 * @param matchList
	 * @param matchInteret
	 * @return
	 */
	public static String[] calCategoryInterest(List<MatchedInterests> matchList,AbstractMatchInterest matchInteret) {
		String[] rs = new String[3];
		List<MatchedInterests> productList = new LinkedList<MatchedInterests>();
		List<MatchedInterests> smallList = new LinkedList<MatchedInterests>();
		List<MatchedInterests> bigList = new LinkedList<MatchedInterests>();
		for (MatchedInterests o : matchList) {
			if (o.getProductId() != 0) {
				productList.add(o);
			} else if (o.getSmallId() != 0) {
				smallList.add(o);
			} else if (o.getBigId() != 0) {
				bigList.add(o);
			}
		}
		Collections.sort(productList);
		Collections.sort(smallList);
		Collections.sort(bigList);

			if (!productList.isEmpty()) {
				rs[0] = productList.get(0).getInterest();
			}
			if (!smallList.isEmpty()) {
				rs[1] = smallList.get(0).getInterest();

			} else if(!productList.isEmpty()){ // 找到产品对应的小靶点，并取相关度得分最高的作为小靶点类兴趣点
				rs[1] =matchInteret.getSmallInterestFromProduct(productList.get(0).getProductId());
			}

			if (!bigList.isEmpty()) {
				rs[2] = bigList.get(0).getInterest();
			} else{
				int id = 0;
				if (!productList.isEmpty() && !smallList.isEmpty()) { // 如果搜索到产品、小靶点的兴趣点，没有大靶点的，从那两个里兴趣点分数高的推测
					if (productList.get(0).getScore() > smallList.get(0).getScore()) {
						id = productList.get(0).getProductId();
						rs[2] = matchInteret.getBigInterestFromProduct(id);
					} else {
						id = smallList.get(0).getSmallId();
						rs[2] = matchInteret.getBigInterestFromSmall(id);
					}
				} else if (!smallList.isEmpty()) {
					id = smallList.get(0).getSmallId();
					rs[2] = matchInteret.getBigInterestFromSmall(id);
				} else if (!productList.isEmpty()) {
					id = productList.get(0).getProductId();
					rs[2] = matchInteret.getBigInterestFromProduct(id);
				}
			}
			
		// System.out.println("interests: "+rs[0]+" "+rs[1]+" "+rs[2]);
		return rs;

	}
	
	/**
	 * 搜索关键字的缩写，分别update Article的assumedKeyWords，definiteKeyWords
	 * 形如：   aaa bbb ccc,ABC| aaa bbb ccc,ABC| aaa bbb ccc,ABC
	 * @param word 搜索的内容
	 * @param article 需要更新的文章
	 */
	private static void findShortKeyWord(String word , Article article , IArticleService articleService ) {
		Pattern p = null;
		Matcher matcher = null;
		for (Iterator<DictClass> iter = MatchKeywordOrAbstractForPMC.DICT_LIST.iterator(); iter.hasNext();) {
			DictClass dc = iter.next();
			String dicKey = dc.getKeyword(); // 字典表中的keyword
			// 查找关键字的缩写
			p = Pattern.compile("(\\b|[^a-zA-Z0-9])" + dicKey + "([ \\-]+\\w*){0,3}[ \\-]*\\(\\w{2,}\\)",Pattern.CASE_INSENSITIVE);
			// 去除标点 CYP3A4: CYP3A4. Additionally, N-desethylamiodarone (DEA)
			// String regex ="\\([A-Za-z0-9]{2,}\\)";
			// Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
			matcher = p.matcher(word);
			while (matcher.find()) {
				String shortKeyWord = matcher.group(); // 形如: aaa bbb ccc (ABC)
				if(shortKeyWord.startsWith(")") || shortKeyWord.startsWith("(")){
					shortKeyWord = shortKeyWord.substring(1);
				}
				String shortWord = shortKeyWord.substring(shortKeyWord.indexOf("(") + 1,shortKeyWord.indexOf(")")); // 缩写，形如: ABC
				
				// 如果缩写全是数字，不算做缩写
				Pattern pattern = Pattern.compile("[0-9]*");
				if(pattern.matcher(shortWord).matches()){
					continue;
				}
				
				// 如果缩写已经是关键字，不算做缩写
				int dictSize = MatchKeywordOrAbstractForPMC.DICT_LIST.size();
				boolean d = false;
				for(int i =0;i<dictSize;i++){
					if(MatchKeywordOrAbstractForPMC.DICT_LIST.get(i).getKeyword().toLowerCase().equals(shortWord.toLowerCase())){
						d = true;
						break;
					}
				}
				if(d){
					continue;
				}
				
				String longWord = shortKeyWord.substring(0,shortKeyWord.indexOf("(")).trim(); // 全称，形如: aaa bbb ccc
				String[] longWords = longWord.split("[ \\-]+");
				boolean b = true; // 是否肯定是关键词的缩写
				// 如果缩写的每个字母和之前的词组的每个单词的开头字母一样，算作肯定是关键字的缩写。
				// 如果不是，算作待定关键字（assumedKeyWords），记录在表里，人工决定
				if (longWords.length == shortWord.length()) {
					for (int i = 0; i < longWords.length; i++) {
						if (!longWords[i].isEmpty() && !longWords[i].substring(0, 1).equalsIgnoreCase(shortWord.substring(i, i + 1))) {
							b = false;
							break;
						}
					}
				} else {
					b = false;
				}
				if (b) {
					// 一定是缩写的，直接当关键词来搜索
					article.setDefiniteKeyWords(article.getDefiniteKeyWords()+longWord + "," + shortWord+"|");
				} else {
					// 可能是缩写的，需要人工判断
					article.setAssumedKeyWords(article.getAssumedKeyWords()+longWord + "," + shortWord+"|");
				}
				
				
			}	
		}
		// System.out.println("DefiniteKeyWords: "+article.getDefiniteKeyWords());
		// System.out.println("AssumedKeyWords: "+article.getAssumedKeyWords());
	}
	
	/**
	 * 把字符日期转成GregorianCalendar
	 * @param pDate 形如 JAN 15 2014 ， 有可能没有月或日
	 * @return
	 */
	private static GregorianCalendar parseDate(String pDate){
		int year = 0;
		int month = 0;
		int day = 1;
		String[] strs = pDate.trim().split(" ");
		if(strs.length == 1){
			year = Integer.valueOf(strs[0]);
		}else if(strs.length == 2){
			year = Integer.valueOf(strs[1]);
			month = Integer.valueOf(getMonthFromStr(strs[0]));
		}else if(strs.length == 3){
			year = Integer.valueOf(strs[2]);
			month = Integer.valueOf(getMonthFromStr(strs[0]));
			day =  Integer.valueOf(strs[1]);
		}
		return new GregorianCalendar(year,month,day);
	}
	
	/**
	 * 从wos上找出来的月份缩写
	 * JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC
	 * @return
	 */
	private static int getMonthFromStr(String month){
		if(month.equalsIgnoreCase("JAN")){
			return GregorianCalendar.JANUARY;
		}else if(month.equalsIgnoreCase("FEB")){
			return GregorianCalendar.FEBRUARY;
		}else if(month.equalsIgnoreCase("MAR")){
			return GregorianCalendar.MARCH;
		}else if(month.equalsIgnoreCase("APR")){
			return GregorianCalendar.APRIL;
		}else if(month.equalsIgnoreCase("MAY")){
			return GregorianCalendar.MAY;
		}else if(month.equalsIgnoreCase("JUN")){
			return GregorianCalendar.JUNE;
		}else if(month.equalsIgnoreCase("JUL")){
			return GregorianCalendar.JULY;
		}else if(month.equalsIgnoreCase("AUG")){
			return GregorianCalendar.AUGUST;
		}else if(month.equalsIgnoreCase("SEP")){
			return GregorianCalendar.SEPTEMBER;
		}else if(month.equalsIgnoreCase("OCT")){
			return GregorianCalendar.OCTOBER;
		}else if(month.equalsIgnoreCase("NOV")){
			return GregorianCalendar.NOVEMBER;
		}else if(month.equalsIgnoreCase("DEC")){
			return GregorianCalendar.DECEMBER;
		}
		return 0;
	}


	// 取数据库表数据brand_dict
	public static List<DictClass> getDict() {
		List<DictClass> dictMap = null;
		IDictService dictService = new DictServiceImpl();
		dictMap = dictService.selectByExample(new Criteria());
		return dictMap;
	}
	

	// 取数产品表selleck_product
	public static Map<Integer, ProductClass> getProducts() {
		IProductService productService = new ProductServiceImpl();
		Map<Integer, ProductClass> products = productService.getProducts();
		System.out.println("products size: "+products.size());
		return products;
	}

	// 取数产品与小靶点关系表 selleck_producttocategory
	public static List<RelClass> getProductCategoryRel() {
		IProductService productService = new ProductServiceImpl();
		List<RelClass> rels = productService.getProductCategoryRel();
		System.out.println("ProductCategoryRel size: "+rels.size());
		return rels;
	}

	// 取数小靶点与大靶点关系表 selleck_productcategory_rel
	public static List<RelClass> getSmallBigRel() {
		IProductService productService = new ProductServiceImpl();
		List<RelClass> rels = productService.getSmallBigRel();
		System.out.println("SmallBigRel size: "+rels.size());
		return productService.getSmallBigRel();
	}

}
