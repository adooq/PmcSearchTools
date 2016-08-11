package selleck.email.pojo;

/**
 * WOS 文章
 * @author  fscai
 *
 */
public class CNKI {
	private int id;
	private String title;
	private String abs;
	private String abs_en;
	private String authors_cn;
	private String authors_en;
	private String foundation;
	private String organization;
	private String keyword;
	private String email;
	private String publication;
	private String pdf_url;
	private String readFlag = "未分析";
	
	public String getReadFlag() {
		return readFlag;
	}
	public void setReadFlag(String readFlag) {
		this.readFlag = readFlag;
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
	public String getAuthors_cn() {
		return authors_cn;
	}
	public void setAuthors_cn(String authors_cn) {
		this.authors_cn = authors_cn;
	}
	public String getAuthors_en() {
		return authors_en;
	}
	public void setAuthors_en(String authors_en) {
		this.authors_en = authors_en;
	}
	public String getFoundation() {
		return foundation;
	}
	public void setFoundation(String foundation) {
		this.foundation = foundation;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
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
	public String getPublication() {
		return publication;
	}
	public void setPublication(String publication) {
		this.publication = publication;
	}
	public String getPdf_url() {
		return pdf_url;
	}
	public void setPdf_url(String pdf_url) {
		this.pdf_url = pdf_url;
	}
	public String getAbs_en() {
		return abs_en;
	}
	public void setAbs_en(String abs_en) {
		this.abs_en = abs_en;
	}

	
	
}
