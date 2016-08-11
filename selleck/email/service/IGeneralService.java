package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.AuthorEmailSuffix;
import selleck.email.pojo.OrganizationSuffix;

public interface IGeneralService {
	List<AuthorEmailSuffix> selectAuthorEmailNoSuffix(Criteria criteria);
	List<OrganizationSuffix> selectOrganizationSuffix(Criteria criteria);
	void updateAuthorEmailNoSuffix(AuthorEmailSuffix aes);
}
