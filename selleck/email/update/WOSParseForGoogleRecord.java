package selleck.email.update;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;
import selleck.email.pojo.GoogleRecord;
import selleck.email.service.IAuthorService;
import selleck.email.service.IGoogleRecordService;
import selleck.email.service.impl.AuthorServiceImpl;
import selleck.email.service.impl.GoogleRecordServiceImpl;
import selleck.utils.StringUtils;

public class WOSParseForGoogleRecord {
//	public static final List<RelClass> productCategoryRel = getProductCategoryRel(); // 缓存产品和小靶点关系表
//	public static final List<RelClass> smallBigRel = getSmallBigRel(); // 缓存小靶点和大靶点关系表
//	public static final Map<Integer, ProductClass> PRODUCTS = getProducts(); // 缓存产品表
//	public static final Map<Integer, ProductClass> CATEGORYS = getCategorys(); // 缓存靶点表

	public static void main(String[] args) {
		parseWOS();
	}

	/**
	 * 252 上的google_record中非通讯作者的email，补充到selleck_edm_author中，
	 * 并加入兴趣selleck_edm_interest
	 * 
	 * @author fscai
	 * 
	 */
	private static void parseWOS() {
		IGoogleRecordService grService = new GoogleRecordServiceImpl();
		IAuthorService authorService = new AuthorServiceImpl();
//		IArticleService articleService = new ArticleServiceImpl();
//		IAuthorInterestService aiService = new AuthorInterestServiceImpl();

		int startIndex =  1;
		int step = 10000;
		while (startIndex <= 3000000) { // select max(id) from google_record
			Criteria criteria = new Criteria();
			// criteria.setOracleStart(startIndex);
			// criteria.setOracleEnd(1000);
			criteria.setWhereClause(" id >= " + startIndex + " and id < " + (startIndex + step));
			startIndex += step;
			// criteria.put("have_read", 0);
			List<GoogleRecord> googleRecordList = grService.selectByCriteria(criteria);
			// System.out.println("wosList "+wosList.size());
					Criteria criteria1 = new Criteria();
					for (GoogleRecord gr : googleRecordList) {
						try {
						System.out.print("gr id: "+gr.getId());
						// System.out.println("gr full_name: "+gr.getFullName());
						// 作者表中是否有相同的作者名和(地址或email）。
						// 如果有了，什么都不做，如果没有，把作者的email更新成google_record中的email
						criteria1.setWhereClause("full_name = '" + StringUtils.toSqlForm(splitName(gr.getFullName()))	+ "' and (LOCATE('" + StringUtils.toSqlForm(gr.getAddress()) + "', address) != 0 or email = '"+gr.getEmail()+"')");
						List<Author> authors = authorService.selectByExample(criteria1);
						// System.out.println("authors size: "+authors.size());
						Author selectedAuthor = null;
						if(authors.size() == 0){
							selectedAuthor = new Author();
							selectedAuthor.setAddress(gr.getAddress());
							selectedAuthor.setEmail(gr.getEmail());
							selectedAuthor.setFullName(splitName(gr.getFullName()));
							selectedAuthor.setShortName(gr.getShortName());
							selectedAuthor.setSource("google record");
							authorService.insertAuthor(selectedAuthor);
							System.out.println(" insert author id: "+selectedAuthor.getId());
						}else{
							boolean originalEmail = false; // 作者表中相同的作者名和地址是否已经有email
							for (Author author : authors) {
								if (author.getEmail() != null && !author.getEmail().isEmpty()) {
									// System.out.println("author.getEmail()  "+author.getEmail());
									originalEmail = true;
								}
							}
							// 如果已经有email，不更新这个email
							if (originalEmail) {
								System.out.println(" originalEmail ");
								continue;
							}
							
							// List<Article> articles = new ArrayList<Article>(); // 同名同地址的作者的所有文章
							for (Author author : authors) {
								// 更新同名同地址的作者的email
								author.setEmail(gr.getEmail());
								System.out.println("update author's email: "+author.getId());
								authorService.updateAuthor(author);
								
								selectedAuthor = author;
								// articles.addAll(articleService.findArticleByAuthor(selectedAuthor));
								
							}
							
							/* 目前文章未定兴趣,不加入作者兴趣表
							Collections.sort(articles); // 从小到大排序
							Collections.reverse(articles);  // 从大到小
							//找作者的文章的兴趣作为作者的兴趣
							AuthorInterest a1 = null;
							AuthorInterest a2 = null;
							if(articles.size() >= 1){
								Article article1 = articles.get(0);
								if(article1.getScore() == 0){ // 去除文章打分0分的（很有可能是没打过分）
									continue;
								}
								a1 = new AuthorInterest();
								a1.setAuthorId(selectedAuthor.getId());
								a1.setBig(article1.getBig());
								a1.setProduct(article1.getProduct());
								a1.setSmall(article1.getSmall());
								a1.setScore(article1.getScore());
							}
							if(articles.size() >= 2){
								Article article2 = articles.get(1);
								if(article2.getScore() == 0){ // 去除文章打分0分的（很有可能是没打过分）
									continue;
								}
								a2 = new AuthorInterest();
								a2.setAuthorId(selectedAuthor.getId());	
								a2.setBig(article2.getBig());
								a2.setProduct(article2.getProduct());
								a2.setSmall(article2.getSmall());
								a2.setScore(article2.getScore());
							}
							aiService.saveAuthorInterest(a1, a2);
							System.out.println("  insert  AuthorInterest ");
							*/
						}
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						/* google_record表里定的兴趣有错乱，占传民做的历史遗留问题，定的兴趣不可信，改为找作者的文章的兴趣作为作者的兴趣
						// 取一个作者放入兴趣表		
						AuthorInterest a1 = new AuthorInterest();
						a1.setAuthorId(selectedAuthor.getId());
						
						if((gr.getMatchKeys() == null && gr.getMatchKeys().isEmpty())){
							a1.setMatchKeys(gr.getMatchKeys());
						}
						if(gr.getInterests() == null && gr.getInterests().isEmpty()){
							a1.setInterests(gr.getInterests());
						}
						
						int productId = gr.getProduct();
						int smallId = gr.getSmall();
						int bigId = gr.getBig();
						
						if (productId != 0) {
							String product = PRODUCTS.get(productId).getName();
							a1.setProduct(product);
						}
						if (smallId == 0) {
							if (productId != 0) {
								Object[] small = getSmallInterestFromProduct(productId);
								a1.setSmall(small[1].toString());
								smallId = Integer.valueOf(small[0].toString());
							}
						} else {
							String small = CATEGORYS.get(smallId).getName();
							a1.setSmall(small);
						}
						if (bigId == 0) {
							if (smallId != 0) {
								Object[] big = getBigInterestFromSmall(smallId);
								bigId = Integer.valueOf(big[0].toString());
								a1.setBig(big[1].toString());
							}
						} else {
							String big = CATEGORYS.get(bigId).getName();
							a1.setBig(big);
						}
						
						a1.setMatchKeys(gr.getMatchKeys());
						a1.setInterests(gr.getInterests());
						
						aiService.saveAuthorInterest(a1, null);
						*/
					}
				
		}
	}

	/**
	 * 有些记录的作者全名full_author不正确，例如：Bode, AnnM. 应该是Bode, Ann M.
	 * 
	 * @param name
	 *            Bode, AnnM.
	 * @return Bode, Ann M.
	 */
	private static String splitName(String name) {
		Pattern p = Pattern.compile("\\B[A-Z]\\.", Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(name);
		while (matcher.find()) {
			name = name.replace(matcher.group(), " " + matcher.group());
		}
		name = name.replaceAll(",", ", "); // 为,后面加空格，为了符合英文书写规范和美观
		name = name.replaceAll("\\s+", " ");
		return name;
	}

	/**
	 * 从authors中去查找CorrespondingAuthor的全名 例如CorrespondingAuthor是 Yu, DS
	 * ，authors是
	 * Ping,SY(Ping,Szu-Yuan)[2]|Wu,CL(Wu,Chia-Lun)[1]|Yu,DS(Yu,Dah-Shyong)[1]
	 * return Yu,Dah-Shyong
	 * 
	 * @param cName
	 *            通讯作者名
	 * @param authorNames
	 *            所有作者的名字
	 * @return
	 */
	private static String findFullNameByCorrespondingAuthor(String cName,
			String authorNames) {
		if (cName == null || cName.isEmpty()) {
			return "";
		}
		int cNameIndex = authorNames.indexOf(cName.replaceAll("\\s", ""));
		if (cNameIndex == -1) {
			return "";
		}
		String tmp = authorNames.substring(cNameIndex);
		return tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")"));
	}

	// 与兴趣点相关的，目前暂时不需要了
//	// 取产品表selleck_product
//	public static Map<Integer, ProductClass> getProducts() {
//		IProductService productService = new ProductServiceImpl();
//		Map<Integer, ProductClass> products = productService.getProducts();
//		System.out.println("products size: " + products.size());
//		return products;
//	}
//
//	// 取靶点表selleck_productcategory
//	public static Map<Integer, ProductClass> getCategorys() {
//		IProductService productService = new ProductServiceImpl();
//		Map<Integer, ProductClass> categorys = productService.getCategoryMap();
//		System.out.println("categorys size: " + categorys.size());
//		return categorys;
//	}
//
//	// 取产品与小靶点关系表 selleck_producttocategory
//	public static List<RelClass> getProductCategoryRel() {
//		IProductService productService = new ProductServiceImpl();
//		List<RelClass> rels = productService.getProductCategoryRel();
//		System.out.println("ProductCategoryRel size: " + rels.size());
//		return rels;
//	}
//
//	// 取小靶点与大靶点关系表 selleck_productcategory_rel
//	public static List<RelClass> getSmallBigRel() {
//		IProductService productService = new ProductServiceImpl();
//		List<RelClass> rels = productService.getSmallBigRel();
//		System.out.println("SmallBigRel size: " + rels.size());
//		return productService.getSmallBigRel();
//	}
//
//	/**
//	 * 通过产品兴趣点，推测小靶点兴趣点
//	 * 
//	 * @param productId
//	 * @return Object[0] 小靶点id Object[1] 小靶点名称
//	 */
//	public static Object[] getSmallInterestFromProduct(int productId) {
//		int id = 0;
//		int maxScore = 0;
//		List<Integer[]> smallIdList = getSmallIdScoreByProductId(productId);
//		for (Iterator<Integer[]> iter = smallIdList.iterator(); iter.hasNext();) {
//			Integer[] smallIdScore = iter.next();
//			// System.out.println("getSmallInterestFromProduct: "+smallIdScore[0]+","+smallIdScore[1]);
//			if (smallIdScore[1] >= maxScore) {
//				id = smallIdScore[0];
//				maxScore = smallIdScore[1];
//			}
//		}
//		Object[] rs = { id, CATEGORYS.get(id).getName() };
//		return rs;
//	}
//
//	/**
//	 * 通过productid 获得所有对应的小靶点id和score
//	 * 
//	 * @param pid
//	 * @return List<Integer[]> Integer[0] smallId Integer[1]score
//	 */
//	public static List<Integer[]> getSmallIdScoreByProductId(int pid) {
//		List<Integer[]> list = new ArrayList<Integer[]>();
//		for (Iterator<RelClass> iter = MatchKeywordOrAbstractTest.productCategoryRel
//				.iterator(); iter.hasNext();) {
//			RelClass rc = iter.next();
//			if (rc.getChildId() == pid) {
//				Integer[] t = { rc.getParentId(), rc.getScore() };
//				list.add(t);
//			}
//		}
//		return list;
//	}
//
//	/**
//	 * 通过小靶点兴趣点，推测大靶点兴趣点
//	 * 
//	 * @param productId
//	 *            * @return Object[0] 大靶点id Object[1] 大靶点名称
//	 */
//	public static Object[] getBigInterestFromSmall(int smallId) {
//		int id = 0;
//		int maxScore = 0;
//		List<Integer[]> bigIdList = getBigIdScoreBySmallId(smallId);
//		for (Iterator<Integer[]> iter = bigIdList.iterator(); iter.hasNext();) {
//			Integer[] bigIdScore = iter.next();
//			// System.out.println("getBigInterestFromSmall: "+bigIdScore[0]+","+bigIdScore[1]);
//			if (bigIdScore[1] >= maxScore) {
//				id = bigIdScore[0];
//				maxScore = bigIdScore[1];
//			}
//		}
//		Object[] rs = { id, CATEGORYS.get(id).getName() };
//		return rs;
//	}
//
//	/**
//	 * 通过小靶点id 获得所有对应的大靶点id和score
//	 * 
//	 * @param pid
//	 * @return List<Integer[]> Integer[0] bigId , Integer[1] score
//	 */
//	public static List<Integer[]> getBigIdScoreBySmallId(int sid) {
//		List<Integer[]> list = new ArrayList<Integer[]>();
//		for (Iterator<RelClass> iter = MatchKeywordOrAbstractTest.smallBigRel
//				.iterator(); iter.hasNext();) {
//			RelClass rc = iter.next();
//			if (rc.getChildId() == sid) {
//				Integer[] t = { rc.getParentId(), rc.getScore() };
//				list.add(t);
//			}
//		}
//		return list;
//	}

}
