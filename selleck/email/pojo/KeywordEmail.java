package selleck.email.pojo;

/**
 * 按关键词搜索的程序，在5大库里都没有找到，直接访问链接获得的email。
 * @author fscai
 *
 */
public class KeywordEmail {
	private String email;
	private String url;
	private String keyword;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	
}
