package selleck.email.pojo;

public class Organization {
	private int id;
	private String address;
	private String organization;
	private String prefix;
	private String country;

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

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}


	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public int hashCode() {
		return prefix.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Organization) {
			return ((Organization) obj).getPrefix().equals(prefix);
		} else {
			return false;
		}
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
}
