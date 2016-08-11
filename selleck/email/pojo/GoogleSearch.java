package selleck.email.pojo;


public class GoogleSearch {
	private int id;
	private int titleId;
	private int realId;
	private String htmlTitle; // 邮箱后缀
	private String title;
	private String keywords;
	private String fullAuthor;
	private String address;
	private String email;
	private String pickDate;
	private String matchKeys;
	private String interests;
	private int small;
	private int big;
	private int product;
	private String dictKeys;
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GoogleSearch) {
			if (((GoogleSearch) obj).getHtmlTitle().equals(this.getHtmlTitle()) && 
					((GoogleSearch) obj).getFullAuthor().equals(this.getFullAuthor()) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + getHtmlTitle().hashCode();
		result = PRIME * result + getFullAuthor().hashCode();
		return result;

	}
	
	@Override
	public String toString() {
		return id+","+htmlTitle+","+fullAuthor+","+realId;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getHtmlTitle() {
		return htmlTitle;
	}
	public void setHtmlTitle(String htmlTitle) {
		this.htmlTitle = htmlTitle;
	}
	public String getFullAuthor() {
		return fullAuthor;
	}
	public void setFullAuthor(String fullAuthor) {
		this.fullAuthor = fullAuthor;
	}
	public int getRealId() {
		return realId;
	}
	public void setRealId(int realId) {
		this.realId = realId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getTitleId() {
		return titleId;
	}

	public void setTitleId(int titleId) {
		this.titleId = titleId;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPickDate() {
		return pickDate;
	}

	public void setPickDate(String pickDate) {
		this.pickDate = pickDate;
	}

	public String getMatchKeys() {
		return matchKeys;
	}

	public void setMatchKeys(String matchKeys) {
		this.matchKeys = matchKeys;
	}

	public String getInterests() {
		return interests;
	}

	public void setInterests(String interests) {
		this.interests = interests;
	}

	public int getSmall() {
		return small;
	}

	public void setSmall(int small) {
		this.small = small;
	}

	public int getBig() {
		return big;
	}

	public void setBig(int big) {
		this.big = big;
	}

	public int getProduct() {
		return product;
	}

	public void setProduct(int product) {
		this.product = product;
	}

	public String getDictKeys() {
		return dictKeys;
	}

	public void setDictKeys(String dictKeys) {
		this.dictKeys = dictKeys;
	}
	
	
}
