package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleSearchEmail;

public interface IGoogleSearchEmailService {
	List<GoogleSearchEmail> selectGSEmailByCriteria(Criteria criteria);
	
	void updateFullAuthor(GoogleSearchEmail gse);
	
	void updateGetEmail(GoogleSearchEmail gse);
}
