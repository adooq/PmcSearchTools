package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleSearchEmail;

public interface GoogleSearchEmailMapper {
	List<GoogleSearchEmail> selectGSEmailByCriteria(Criteria criteria);
	
	void updateFullAuthor(GoogleSearchEmail gse);
	
	void updateGetEmail(GoogleSearchEmail gse);
}
