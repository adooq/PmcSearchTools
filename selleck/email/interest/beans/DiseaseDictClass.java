package selleck.email.interest.beans;

public class DiseaseDictClass {
	private int id;
	private String keyword;
	private String disease;
	private String category;
	private String reference;
	private String selleck;
	private int priority = 9; // 优先级 从小到大从低到高。如果优先级高的搜索到了，就不搜索优先级低的了
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public String getDisease() {
		return disease;
	}
	public void setDisease(String disease) {
		this.disease = disease;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getSelleck() {
		return selleck;
	}
	public void setSelleck(String selleck) {
		this.selleck = selleck;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
}

