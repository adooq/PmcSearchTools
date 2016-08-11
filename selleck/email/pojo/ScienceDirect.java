package selleck.email.pojo;

/**
 * PMC 文章
 * @author fscai
 *
 */
public class ScienceDirect {
	private int id;
	private String title;
	private String abs; // 页面上叫summary
	private String keyword; // 页面上叫Highlights
	private String correspondingAuthor;
	private String correspondingAddress;
	private String authors;
	private String addresses;
	private String email;
	private String sourcePublication;
	private String pDate;
	private String reference; // 参考资料
	private String fullText; // 全文
	private String url;
	private byte haveRead;
	
	// for search_sciencedirect_by_keyword
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
	public String getFullText() {
		return fullText;
	}
	public void setFullText(String fullText) {
		this.fullText = fullText;
	}
	public byte getHaveRead() {
		return haveRead;
	}
	public void setHaveRead(byte haveRead) {
		this.haveRead = haveRead;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getSearchKeyword() {
		return searchKeyword;
	}
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	
	
}
