package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.AuthorInterest;


public interface AuthorInterestMapper {
	void saveAuthorInterest(AuthorInterest ai);
	List<AuthorInterest> getAuthorInterest(Criteria criteria);
}
