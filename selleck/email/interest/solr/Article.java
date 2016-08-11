package selleck.email.interest.solr;

import org.apache.solr.client.solrj.beans.Field;

public class Article {
	@Field
	private String id;
	
	@Field("title_en_wos")
	private String title;
	
	@Field("abstract_en_wos")
	private String abs;
	
	@Field("keyword_en_wos")
	private String keyword;
	
	private String keywordPlus;
	
	@Field("c_author_en_wos")
	private String correspondingAuthor;
	
	@Field("c_address_en_wos")
	private String correspondingAddress;
	
	@Field("authors_en_wos")
	private String authors;
	private String addresses;
	
	@Field("email_en_wos")
	private String email;
	
	@Field("publication_en_wos")
	private String sourcePublication;
	private String pDate;
	private String source;
	
	
	// =======  wos 特有 =========
	private String type;
	private String classification;
	private String research;
	// ========================
	
	
	// ======== PMC 特有 ========
	private String referrence;
	
	@Field("full_text_en_wos")
	private String fullText;
	
	@Field("c_info_en_wos")
	private String correspondingInfo;
	// ========================
	
	

	public String getId() {
		return id;
	}
	public void setId(String id) {
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
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	
	
}
