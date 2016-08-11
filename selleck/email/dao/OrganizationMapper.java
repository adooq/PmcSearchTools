package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.Organization;

public interface OrganizationMapper {

	List<Organization> selectByExample(Criteria criteria);

	void updateOrganization(Organization organization);

	void insertOrganization(Organization organization);

	void saveOrganization(Organization organization);

	void deleteOrganization(Organization organ);

}
