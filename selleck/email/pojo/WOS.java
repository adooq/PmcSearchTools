package selleck.email.pojo;

/**
 * WOS 文章
 * @author  fscai
 *
 */
public class WOS {
	private int id;
	private String title;
	private String abs;
	private String keyword;
	private String keywordPlus;
	private String correspondingAuthor;
	private String correspondingAddress;
	private String authors;
	private String addresses;
	private String email;
	private String sourcePublication;
	private String pDate;
	private String type;
	private String classification;
	private String research;
	private byte haveRead;
	
	// for search_wos_by_keyword
	private String searchKeyword;
	
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
	public String getAuthors() {
		return authors;
	}
	public void setAuthors(String authors) {
		this.authors = authors;
	}
	public String getAddresses() {
		return addresses;
	}
	public void setAddresses(String addresses) {
		this.addresses = addresses;
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
	public String getpDate() {
		return pDate;
	}
	public void setpDate(String pDate) {
		this.pDate = pDate;
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
	public byte getHaveRead() {
		return haveRead;
	}
	public void setHaveRead(byte haveRead) {
		this.haveRead = haveRead;
	}
	public String getSearchKeyword() {
		return searchKeyword;
	}
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	
}
