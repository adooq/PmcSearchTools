package selleck.email.interest.beans;

/**
 * 
 * @author fscai
 *
 */
public class ProductClass {
	private int id;
	private String name;
	private String cat; // cat号
	private int smallCategoryId;
	private int bigCategoryId;
	private String sql_name;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSmallCategoryId() {
		return smallCategoryId;
	}
	public void setSmallCategoryId(int smallCategoryId) {
		this.smallCategoryId = smallCategoryId;
	}
	public int getBigCategoryId() {
		return bigCategoryId;
	}
	public void setBigCategoryId(int bigCategoryId) {
		this.bigCategoryId = bigCategoryId;
	}
	public String getSql_name() {
		return sql_name;
	}
	public void setSql_name(String sql_name) {
		this.sql_name = sql_name;
	}
	public String getCat() {
		return cat;
	}
	public void setCat(String cat) {
		this.cat = cat;
	}
	
	
}
