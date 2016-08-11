package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.AuthorInterest;


public interface IAuthorInterestService {
	void saveAuthorInterest(AuthorInterest a1,AuthorInterest a2);
	List<AuthorInterest> getAuthorInterest(Criteria criteria);

}
