package selleck.email.pojo;

import selleck.email.update.tools.ParserUtils;

public class Author {
	private int id;
	private String fullName;
	private String shortName;
	private String address;
	private String organization;
	private String email;
	private String source;
	private String country;
	private int organ;

	// 程序需要在Author中记录一下，最终还是存到selleck_edm_article_author_rel表里
	private Integer priority = 3; // 作者在这篇文章中的重要性。
									// 0通讯作者，1第一作者(作者中名字排第一个)，2第二作者(作者中名字排第二个)，3什么都不是

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		// this.email = email;
		if ("" == email || null == email) {
			return;
		} else {
			this.email = ParserUtils.cleanEmail(email); // 去除不符合邮箱格式的部分
		}
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public int getOrgan() {
		return organ;
	}

	public void setOrgan(int organ) {
		this.organ = organ;
	}

}
