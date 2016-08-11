package selleck.email.pojo;

public class Article implements Comparable<Article>{
	private int id;
	private String title;
	private String titleIndex;
	private String abs;
	private String keyword;
	private String keywordPlus;
	private String email;
	private String sourcePublication;
	private String pDate;
	private String type;
	private String classification;
	private String research;
	private String source;
	private String correspondingAuthor; // CORRESPONDINGAUTHOR
	private String fullCorrespondingAuthor; // 通讯作者全名
	private String correspondingAddress; // CORRESPONDINGADDRESS
	private String assumedKeyWords = ""; // 待定关键词  aaa bbb ccc,ABC| aaa bbb ccc,ABC| aaa bbb ccc,ABC
	private String definiteKeyWords = ""; // 确定关键词  aaa bbb ccc,ABC| aaa bbb ccc,ABC| aaa bbb ccc,ABC
	private String product;  // 产品兴趣
	private String small; // 小靶点兴趣
	private String big; // 大靶点兴趣
	private int score;
	private byte passed;  // 是否通过某些特殊的筛选条件   1通过   0未通过
	private byte parsed;  // 是否已经分析过兴趣点  0为分析  1已分析
	
	// PMC 新增字段
	private String referrence; // 参考资料
	private String fullText; // 全文
	private String correspondingInfo; // 通信信息部分
	
	//不是数据库列，只是为程序方便临时使用
	private int authorId;
	
	@Override
	public int compareTo(Article o) {
		return this.getScore() - o.getScore();
	}
	
	@Override
	public String toString() {
		return "("+id+","+score+")";
	}



	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAbs() {
		return abs;
	}
	public void setAbs(String abs) {
		this.abs = abs;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getSourcePublication() {
		return sourcePublication;
	}
	public void setSourcePublication(String sourcePublication) {
		this.sourcePublication = sourcePublication;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getClassification() {
		return classification;
	}
	public void setClassification(String classification) {
		this.classification = classification;
	}
	public String getResearch() {
		return research;
	}
	public void setResearch(String research) {
		this.research = research;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getTitleIndex() {
		return titleIndex;
	}
	public void setTitleIndex(String titleIndex) {
		this.titleIndex = titleIndex;
	}
	public String getpDate() {
		return pDate;
	}
	public void setpDate(String pDate) {
		this.pDate = pDate;
	}
	public String getKeywordPlus() {
		return keywordPlus;
	}
	public void setKeywordPlus(String keywordPlus) {
		this.keywordPlus = keywordPlus;
	}
	public String getCorrespondingAuthor() {
		return correspondingAuthor;
	}
	public void setCorrespondingAuthor(String correspondingAuthor) {
		this.correspondingAuthor = correspondingAuthor;
	}
	public String getCorrespondingAddress() {
		return correspondingAddress;
	}
	public void setCorrespondingAddress(String correspondingAddress) {
		this.correspondingAddress = correspondingAddress;
	}
	public String getAssumedKeyWords() {
		return assumedKeyWords;
	}
	public void setAssumedKeyWords(String assumedKeyWords) {
		this.assumedKeyWords = assumedKeyWords;
	}
	public String getDefiniteKeyWords() {
		return definiteKeyWords;
	}
	public void setDefiniteKeyWords(String definiteKeyWords) {
		this.definiteKeyWords = definiteKeyWords;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public byte getPassed() {
		return passed;
	}
	public void setPassed(byte passed) {
		this.passed = passed;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public String getSmall() {
		return small;
	}
	public void setSmall(String small) {
		this.small = small;
	}
	public String getBig() {
		return big;
	}
	public void setBig(String big) {
		this.big = big;
	}

	public int getAuthorId() {
		return authorId;
	}

	public void setAuthorId(int authorId) {
		this.authorId = authorId;
	}

	public byte getParsed() {
		return parsed;
	}

	public void setParsed(byte parsed) {
		this.parsed = parsed;
	}

	public String getReferrence() {
		return referrence;
	}

	public void setReferrence(String referrence) {
		this.referrence = referrence;
	}

	public String getFullText() {
		return fullText;
	}

	public void setFullText(String fullText) {
		this.fullText = fullText;
	}

	public String getCorrespondingInfo() {
		return correspondingInfo;
	}

	public void setCorrespondingInfo(String correspondingInfo) {
		this.correspondingInfo = correspondingInfo;
	}

	public String getFullCorrespondingAuthor() {
		return fullCorrespondingAuthor;
	}

	public void setFullCorrespondingAuthor(String fullCorrespondingAuthor) {
		this.fullCorrespondingAuthor = fullCorrespondingAuthor;
	}
	
}
