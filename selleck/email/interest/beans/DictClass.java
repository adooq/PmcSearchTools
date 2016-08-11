package selleck.email.interest.beans;

import java.util.Date;


/**
 * 
 * @author fscai
 *
 */
public class DictClass {
	private int id;
	private String keyword;
	private String interests;
	private int categoryId;
	private Date addDate;
	private int flag;
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DictClass){
			if(((DictClass) obj).getFlag() == this.getFlag() && ((DictClass) obj).getCategoryId() == this.getCategoryId()){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + this.getCategoryId();
		result = PRIME * result + this.getFlag();
		return result;
	}
	
	
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
	public int getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}
	public Date getAddDate() {
		return addDate;
	}
	public void setAddDate(Date addDate) {
		this.addDate = addDate;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public String getInterests() {
		return interests;
	}
	public void setInterests(String interests) {
		this.interests = interests;
	}
	
	
}
