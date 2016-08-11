package selleck.email.pojo;

import java.sql.Timestamp;

public class BrandTitleTemp {
	private int id;
	private String keyword;
	private Timestamp pickDate;
	private String website;
	private String originalTitle;
	private byte haveRead;
	private byte haveFound;
	
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
	public Timestamp getPickDate() {
		return pickDate;
	}
	public void setPickDate(Timestamp pickDate) {
		this.pickDate = pickDate;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	public String getOriginalTitle() {
		return originalTitle;
	}
	public void setOriginalTitle(String originalTitle) {
		this.originalTitle = originalTitle;
	}
	public byte getHaveRead() {
		return haveRead;
	}
	public void setHaveRead(byte haveRead) {
		this.haveRead = haveRead;
	}
	public byte getHaveFound() {
		return haveFound;
	}
	public void setHaveFound(byte haveFound) {
		this.haveFound = haveFound;
	}
	
	
}
