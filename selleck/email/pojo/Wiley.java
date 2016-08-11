package selleck.email.pojo;

/**
 * Wiley 文章
 * @author fscai
 *
 */
public class Wiley {
	private int id;
	private String title;
	private String abs;
	private String correspondingAuthor;
	private String correspondingAddress;
	private String authors;
	private String addresses;
	private String email;
	private String journal;
	private String publicationDate;
	private String keyword;
	private String type;
	private String url;
	private String fullText;
	private String fullTextUrl;
	private String reference;
	private String correspondingInfo;
	private byte haveRead;
	
	// for search_wiley_by_keyword
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
	public byte getHaveRead() {
		return haveRead;
	}
	public void setHaveRead(byte haveRead) {
		this.haveRead = haveRead;
	}
	public String getJournal() {
		return journal;
	}
	public void setJournal(String journal) {
		this.journal = journal;
	}
	public String getPublicationDate() {
		return publicationDate;
	}
	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}
	public String getCorrespondingInfo() {
		return correspondingInfo;
	}
	public void setCorrespondingInfo(String correspondingInfo) {
		this.correspondingInfo = correspondingInfo;
	}
	public String getFullText() {
		return fullText;
	}
	public void setFullText(String fullText) {
		this.fullText = fullText;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getFullTextUrl() {
		return fullTextUrl;
	}
	public void setFullTextUrl(String fullTextUrl) {
		this.fullTextUrl = fullTextUrl;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getSearchKeyword() {
		return searchKeyword;
	}
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	
}
