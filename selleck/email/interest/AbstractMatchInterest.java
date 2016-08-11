package selleck.email.interest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.handle.model.Criteria;
import selleck.email.interest.beans.DictClass;
import selleck.email.interest.beans.MatchedInterests;
import selleck.email.interest.beans.ProductClass;
import selleck.email.interest.beans.RelClass;
import selleck.email.service.IProductService;
import selleck.email.service.impl.ProductServiceImpl;

public abstract class AbstractMatchInterest {
	String escapeChar = "\\(|\\)|\\{|\\}|\\[|\\]|,|/|:|\\.|\\*|;";
	
	int productPoint = 40; // 匹配到产品的加分
	int smallPoint = 10; // 匹配到小靶点的加分
	int bigPoint = 20; // 匹配到大靶点的加分
	int productSmallPoint = 30; // 产品和小靶点匹配到后的兴趣点加分
	int SmallBigPoint = 15; //小靶点和大靶点匹配到后的兴趣点加分
	int productBigPoint = 10; // 产品和大靶点匹配到后的兴趣点加分
	float BASE_NUMBER = 1.2f; // 底数
	
	/**
	 * 确定兴趣点的计算方法
	 * 
	 * @param article
	 *            要在article中寻找
	 * @param originalinterest
	 *            原有的兴趣点 String格式Title19 interests;Title20 small;Title21
	 *            big;Title22 product
	 * @return List<String[]> String[]格式
	 *         matchedKey;interest;small;big;product;dictkeys
	 */
	public List<MatchedInterests> matchWordWithDict(String article ){
				List<String> wordsList = this.splitWord(article);
				// 匹配产品小靶点大靶点,并打分

				List<MatchedInterests> matchList = new ArrayList<MatchedInterests>();
				List<DictClass> totalDictList = new ArrayList<DictClass>();
				totalDictList.addAll(MatchKeywordOrAbstractTest.DICT_LIST);
				for (Iterator<DictClass> iter = MatchKeywordOrAbstractTest.DICT_LIST.iterator(); iter.hasNext();) {
					DictClass dc = iter.next();
					for (String word : wordsList) {
						String dicKey = dc.getKeyword(); // 字典表中的keyword
						Pattern p = null;
						Matcher matcher = null;
						
						// 查找关键字
						String escapedDictKey = dicKey.replaceAll("\\p{Punct}", ".");
						p = Pattern.compile("(\\G|[^a-zA-Z_0-9])"+escapedDictKey+"([^a-zA-Z_0-9]|\\z)",Pattern.CASE_INSENSITIVE);
						matcher = p.matcher(word);
						while(matcher.find()){
							addToMatchList(matcher.group(),dc,matchList);
						}
						
						
					}
				}

				// 如果匹配到的兴趣小于2个，把默认的原兴趣点加上
//				if (matchList.size() < 2) {
//					String[] originals = originalInterest.split(";");
//					String originalInterests = originals[0];
//					String originaSmall = originals[1];
//					String originalBig = originals[2];
//					String originalProduct = originals[3];
//					MatchedInterests ic = new MatchedInterests();
//					ic.setMatchedKey("");
//					ic.setInterest(originalInterests);
//					ic.setSmallId(Integer.valueOf(originaSmall));
//					ic.setBigId(Integer.valueOf(originalBig));
//					ic.setProductId(Integer.valueOf(originalProduct));
//					ic.setDictKey("");
//					matchList.add(ic);
//				}
				
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
				
				// 判断产品和小靶点是否是从属关系，如果是兴趣点各加5分
				if (!productList.isEmpty() && !smallList.isEmpty()) {
					for(Iterator<MatchedInterests> productInterestIter= productList.iterator();productInterestIter.hasNext();){
						MatchedInterests productInterest = productInterestIter.next();
						for(Iterator<MatchedInterests> smallInterestIter = smallList.iterator();smallInterestIter.hasNext();){
							MatchedInterests smallInterest = smallInterestIter.next();
							boolean isRel = this.checkProductAndSmallRel(productInterest.getProductId(),smallInterest.getSmallId());
							if(isRel){
								// System.out.println("产品和小靶点关联加分 " + productInterest.getProductId() + " " + smallInterest.getSmallId()+" score:"+this.productSmallPoint);
								productInterest.addScore(this.getProductSmallPoint());
								smallInterest.addScore(this.getProductSmallPoint());
							}
						}
					}
				}
				// 判断小靶点和大靶点是否是从属关系，如果是兴趣点各加5分
				if (!smallList.isEmpty() && !bigList.isEmpty()) {
					for(Iterator<MatchedInterests> bigInterestIter= bigList.iterator();bigInterestIter.hasNext();){
						MatchedInterests bigInterest = bigInterestIter.next();
						for(Iterator<MatchedInterests> smallInterestIter = smallList.iterator();smallInterestIter.hasNext();){
							MatchedInterests smallInterest = smallInterestIter.next();
							boolean isRel = this.checkSmallAndBiglRel(smallInterest.getSmallId(),bigInterest.getBigId());
							if(isRel){
								// System.out.println("小靶点和大靶点关联加分 " + smallInterest.getSmallId() + " " + bigInterest.getBigId()+" score:"+this.SmallBigPoint);
								smallInterest.addScore(this.getSmallBigPoint());
								bigInterest.addScore(this.getSmallBigPoint());
							}
						}
					}
				}
				
				// 判断产品和大靶点是否是从属关系，如果是兴趣点各加5分
				if (!productList.isEmpty() && !bigList.isEmpty()) {
					for(Iterator<MatchedInterests> productInterestIter= productList.iterator();productInterestIter.hasNext();){
						MatchedInterests productInterest = productInterestIter.next();
						for(Iterator<MatchedInterests> bigInterestIter = bigList.iterator();bigInterestIter.hasNext();){
							MatchedInterests bigInterest = bigInterestIter.next();
							boolean isRel = this.checkProductAndBiglRel(productInterest.getProductId(),bigInterest.getBigId());
							if(isRel){
								// System.out.println("产品和大靶点关联加分" + productInterest.getProductId() + " " + bigInterest.getBigId() + " score:"+this.productBigPoint);
								productInterest.addScore(this.getProductBigPoint());
								bigInterest.addScore(this.getProductBigPoint());
							}
						}
					}
				}
				
				Collections.sort(matchList);
				if(!matchList.isEmpty()){
					MatchedInterests mi1 = matchList.get(0);
					mi1.setIsMaxScore(new Byte("1"));
				}
				
				// MatchedInterests mi2 = matchList.get(1);
				// matchList.clear();
				// matchList.add(mi1);
				// matchList.add(mi2);
				
				
				
				// 打印
//				for (MatchedInterests o : matchList) {
//					System.out
//							.println("matchedKey:" + o.getMatchedKey() + " interest:"
//									+ o.getInterest() + " small:" + o.getSmallId()
//									+ " big:" + o.getBigId() + " product:"
//									+ o.getProductId() + " dictkeys:" + o.getDictKey()
//									+ " score: " + o.getScore());
//				}

				return matchList;
	}
	
	
	// 判断字符是否为数字
		boolean isNumeric(String str) {
			Pattern pattern = Pattern.compile("[0-9]*");
			Matcher isNum = pattern.matcher(str);
			if (!isNum.matches()) {
				return false;
			}
			return true;
		}
		
		/**
		 * 
		 * @param word 匹配到的词
		 * @param dc  字典中的关键字
		 * @param matchList  匹配到的兴趣点list
		 */
		void addToMatchList(String word,DictClass dc,List<MatchedInterests> matchList){
			MatchedInterests ic = new MatchedInterests();
			ic.setMatchedKey(word);
			ic.setInterest(dc.getInterests());
			ic.setDictKey(dc.getKeyword());
			int indexOfMatchList = matchList.indexOf(ic);
			if (indexOfMatchList == -1) { // 如果兴趣点重复，就认为是同一个兴趣点，不再加入matchList。有个副作用是同一个兴趣点可能有由多个MatchedKey搜到，那不同的MatchedKey就记录不到了，虽然这并不重要。
				matchList.add(ic);
			}else{
				ic = null;
				ic = matchList.get(indexOfMatchList);
				ic.count++;
			}
			int dicFlag = dc.getFlag(); //字典表中的flag 1产品 2小靶点 3大靶点  4screenlibrary
			if (dicFlag == 1) {
				ic.setProductId(dc.getCategoryId());
				ic.calScore(this.productPoint,BASE_NUMBER);
			} else if (dicFlag == 2) {
				ic.setSmallId(dc.getCategoryId());
				ic.calScore(this.smallPoint,BASE_NUMBER);
			} else if (dicFlag == 3) {
				ic.setBigId(dc.getCategoryId());
				ic.calScore(this.bigPoint,BASE_NUMBER);
			}

			
		}
		
		/**
		 * 检查productId 和 smallId是否有从属关系
		 * @param childId
		 * @param parentId
		 * @return
		 */
		public boolean checkProductAndSmallRel(Integer childId,Integer parentId){
			if(MatchKeywordOrAbstractTest.productCategoryRel.contains(new RelClass(childId,parentId))){
				return true;
			}
			return false;
		}
		
		/**
		 * 检查smallId 和 bigId是否有从属关系
		 * @param childId
		 * @param parentId
		 * @return
		 */
		public boolean checkSmallAndBiglRel(Integer childId,Integer parentId){
			if(MatchKeywordOrAbstractTest.smallBigRel.contains(new RelClass(childId,parentId))){
				return true;
			}
			return false;
		}
		
		/**
		 * 检查productId 和 bigId是否有从属关系
		 * @param childId
		 * @param parentId
		 * @return
		 */
		public boolean checkProductAndBiglRel(Integer childId,Integer parentId){
			List<Integer> smallList = getSmallIdByProductId(childId);
			for(Iterator<Integer> iter = smallList.iterator();iter.hasNext();){
				if(checkSmallAndBiglRel(iter.next(), parentId)){
					return true;
				}
			}
			
			return false;
		}
		
		
		/**
		 * 通过productid 获得所有对应的小靶点id
		 * @param pid
		 * @return List<Integer>
		 */
		public List<Integer> getSmallIdByProductId(int pid){
			List<Integer> list =  new ArrayList<Integer>();
			for(Iterator<RelClass> iter = MatchKeywordOrAbstractTest.productCategoryRel.iterator();iter.hasNext();){
				RelClass rc = iter.next();
				if(rc.getChildId() == pid){
					list.add(rc.getParentId());
				}
			}
			return list;
		}
		
		/**
		 * 通过productid 获得所有对应的小靶点id和score
		 * @param pid
		 * @return List<Integer[]> Integer[0] smallId   Integer[1]score
		 */
		public List<Integer[]> getSmallIdScoreByProductId(int pid){
			List<Integer[]> list =  new ArrayList<Integer[]>();
			for(Iterator<RelClass> iter = MatchKeywordOrAbstractTest.productCategoryRel.iterator();iter.hasNext();){
				RelClass rc = iter.next();
				if(rc.getChildId() == pid){
					Integer[] t = {rc.getParentId(),rc.getScore()};
					list.add(t);
				}
			}
			return list;
		}
		
		/**
		 * 通过小靶点id 获得所有对应的大靶点id
		 * @param pid
		 * @return List<Integer>
		 */
		public List<Integer> getBigIdBySmallId(int sid){
			List<Integer> list =  new ArrayList<Integer>();
			for(Iterator<RelClass> iter = MatchKeywordOrAbstractTest.smallBigRel.iterator();iter.hasNext();){
				RelClass rc = iter.next();
				if(rc.getChildId() == sid){
					list.add(rc.getParentId());
				}
			}
			return list;
		}
		
		/**
		 * 通过小靶点id 获得所有对应的大靶点id和score
		 * @param pid
		 * @return List<Integer[]> Integer[0] bigId ,  Integer[1] score
		 */
		public List<Integer[]> getBigIdScoreBySmallId(int sid){
			List<Integer[]> list =  new ArrayList<Integer[]>();
			for(Iterator<RelClass> iter = MatchKeywordOrAbstractTest.smallBigRel.iterator();iter.hasNext();){
				RelClass rc = iter.next();
				if(rc.getChildId() == sid){
					Integer[] t = {rc.getParentId(),rc.getScore()};
					list.add(t);
				}
			}
			return list;
		}
		
		/**
		 * 通过产品兴趣点，推测大靶点兴趣点
		 * @param productId
		 */
		public String getBigInterestFromProduct(int productId){
			int id = 0;
			int maxScore = 0;
			List<Integer[]> smallIdList = this.getSmallIdScoreByProductId(productId);
			for(Iterator<Integer[]> iter = smallIdList.iterator();iter.hasNext();){
				Integer[] smallIdScore = iter.next();
				List<Integer[]> bigIdList = this.getBigIdScoreBySmallId(smallIdScore[0]);
				for(Iterator<Integer[]> iter1 = bigIdList.iterator();iter1.hasNext();){
					Integer[] bigIdScore = iter1.next();
					// System.out.println("getBigInterestFromProduct: "+smallIdScore[0]+","+smallIdScore[1]+" "+bigIdScore[0]+","+bigIdScore[1]);
					if(smallIdScore[1]*bigIdScore[1] >= maxScore){
						id = bigIdScore[0];
						maxScore = smallIdScore[1]*bigIdScore[1];
					}
				}
			}
			return this.getInterestByIdFlag(id,3);
		}
		
		/**
		 * 通过小靶点兴趣点，推测大靶点兴趣点
		 * @param productId
		 */
		public String getBigInterestFromSmall(int smallId){
			int id = 0;
			int maxScore = 0;
			List<Integer[]> bigIdList = this.getBigIdScoreBySmallId(smallId);
			for(Iterator<Integer[]> iter = bigIdList.iterator();iter.hasNext();){
				Integer[] bigIdScore = iter.next();
				// System.out.println("getBigInterestFromSmall: "+bigIdScore[0]+","+bigIdScore[1]);
				if(bigIdScore[1] >= maxScore){
					id = bigIdScore[0];
					maxScore = bigIdScore[1];
				}
			}
			return this.getInterestByIdFlag(id,3);
		}
		
		/**
		 * 通过产品兴趣点，推测小靶点兴趣点
		 * @param productId
		 */
		public String getSmallInterestFromProduct(int productId){
			int id = 0;
			int maxScore = 0;
			List<Integer[]> smallIdList = this.getSmallIdScoreByProductId(productId);
			for(Iterator<Integer[]> iter = smallIdList.iterator();iter.hasNext();){
				Integer[] smallIdScore = iter.next();
				// System.out.println("getSmallInterestFromProduct: "+smallIdScore[0]+","+smallIdScore[1]);
				if(smallIdScore[1] >= maxScore){
					id = smallIdScore[0];
					maxScore = smallIdScore[1];
				}
			}
			return this.getInterestByIdFlag(id,2);
		}
		
		/**
		 * 根据id flag，到dictmap中搜索对应的interest
		 * @param id
		 * @param flag
		 * @return
		 */
		public String getInterestByIdFlag(int id,int flag){
			for(Iterator<DictClass> iter = MatchKeywordOrAbstractTest.DICT_LIST.iterator();iter.hasNext();){
				DictClass dc = iter.next();
				if(flag == dc.getFlag() && id == dc.getCategoryId()){
					return dc.getInterests();
				}
			}
			// System.out.println("getInterestByIdFlag , interest not found");
			IProductService productService = new ProductServiceImpl();
			Criteria criteria = new Criteria();
			criteria.setWhereClause(" id = " + id +" limit 1");
			List<ProductClass> cateList = productService.getCategory(criteria);
			if(cateList.size() != 0){
				return cateList.get(0).getName();
			}
			return "Others"; // 找不到情况下默认值。实际情况也的确是大靶点Others，id 14。
		}
		
		
		
		/**
		 * 把要做匹配的文章段落里的词分成单个词的list
		 * @param words 把要做匹配的文章段落
		 * @return
		 */
		abstract List<String> splitWord(String words);
		
		
		/**
		 * wos文章缩写计数。stem cells(SCs) 要记录SCs出现的次数来代表stem cells
		 */
		abstract List<String> findShortKeyword(String word);


		public int getProductSmallPoint() {
			return productSmallPoint;
		}


		public void setProductSmallPoint(int productSmallPoint) {
			this.productSmallPoint = productSmallPoint;
		}


		public int getSmallBigPoint() {
			return SmallBigPoint;
		}


		public void setSmallBigPoint(int smallBigPoint) {
			SmallBigPoint = smallBigPoint;
		}


		public int getProductBigPoint() {
			return productBigPoint;
		}


		public void setProductBigPoint(int productBigPoint) {
			this.productBigPoint = productBigPoint;
		}
		
		
}
