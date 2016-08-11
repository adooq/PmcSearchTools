package selleck.email.controller;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Author;
import selleck.email.service.IAuthorService;
import selleck.email.service.IOrganizationService;
import selleck.email.service.impl.AuthorServiceImpl;
import selleck.email.service.impl.OrganizationServiceImpl;

public class DealOrganization {
	static IOrganizationService organizationService = new OrganizationServiceImpl("life science");
	static IAuthorService authorService = new AuthorServiceImpl("life science");

	public static void main(String[] args) {

		Criteria organCriteria = new Criteria();
		Criteria authorCriteria = new Criteria();

		int step = 20000;
		for (int i = 0; i < 83234236; i += step) {// 83234236
			organCriteria.setTableName("organization");

			authorCriteria.setWhereClause(" id > " + i + " and id <= " + (i + step) + " and organ is null");
			List<Author> authors = authorService.selectByExample(authorCriteria);
			for (Author author : authors) {
				String address = author.getAddress();
				if (!address.contains(",")) {
					continue;
				}
				System.out.println(address);
				try {
					int index = 0;
					if (-1 != address.indexOf(",", address.indexOf(",") + 1)) {
						index = address.indexOf(",", address.indexOf(",") + 1);
					} else {
						index = address.length();
					}
					String prefix = address.substring(0, index);
					prefix = ParseAddress.strictlyTrim(prefix);
//					 Organization organization=new Organization();
//					 organization.setAddress(author.getAddress());
//					 organization.setCountry(author.getCountry());
//					 organization.setOrganization(author.getOrganization());
//					 organization.setPrefix(prefix);
//					 organizationService.saveOrganization(organization);
//					 System.out.println(prefix);
					
					organCriteria.setWhereClause(" prefix = '" + prefix.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\\'", "\\\\\\\'") + "'");
					int organId = organizationService.selectByExample(organCriteria).get(0).getId();
					author.setOrgan(organId);
					System.out.println(author.getId() + "----" + author.getOrgan());
					authorService.updateAuthor(author);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}

			}

		}

	}

}
