package selleck.email.interest.beans;

/**
 * 用来做匹配出来的兴趣结果的封装
 * 
 * @author fscai
 * 
 */
public class MatchedInterests implements Comparable<MatchedInterests> {
	private String matchedKey; // 匹配到的词
	private String interest; // 对应的兴趣点
	private int smallId = 0; // 小靶点id
	private int bigId = 0; // 大靶点id
	private int productId = 0; // 产品 id
	private String dictKey; // 匹配到的字典表中的关键字
	private int score = 0; // 兴趣点打分，用来判断兴趣点的可信度
	private Byte isMaxScore = 0; // 是否是一个用户得分最高的兴趣点
	
	public int count = 1; // 该兴趣点匹配到的次数
	

	public void addScore(int s) {
		this.score += s;
	}

	/**
	 * 计算兴趣点相关程度得分  typePoint*(n的baseNumer次方)  n是兴趣点出现次数
	 * @param typePoint product,small,big的评分
	 * @baseNumber 底数
	 * 
	 */
	public void calScore(int typePoint,float baseNumer) {
		this.setScore(((int)Math.round(typePoint*Math.pow(count,baseNumer))));
		// System.out.println("出现次数加分: " + this.getInterest() + " " + this.getScore());
	}

	@Override
	public int compareTo(MatchedInterests mi) {
		return mi.score - this.score;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MatchedInterests) {
			if (((MatchedInterests) obj).getInterest().equals(this.interest)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + getInterest().hashCode();
		return result;

	}

	public String getMatchedKey() {
		return matchedKey;
	}

	public void setMatchedKey(String matchedKey) {
		this.matchedKey = matchedKey;
	}

	public String getInterest() {
		return interest;
	}

	public void setInterest(String interest) {
		this.interest = interest;
	}

	public String getDictKey() {
		return dictKey;
	}

	public void setDictKey(String dictKey) {
		this.dictKey = dictKey;
	}

	public int getSmallId() {
		return smallId;
	}

	public void setSmallId(int smallId) {
		this.smallId = smallId;
	}

	public int getBigId() {
		return bigId;
	}

	public void setBigId(int bigId) {
		this.bigId = bigId;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Byte getIsMaxScore() {
		return isMaxScore;
	}

	public void setIsMaxScore(Byte isMaxScore) {
		this.isMaxScore = isMaxScore;
	}
	
	
}
