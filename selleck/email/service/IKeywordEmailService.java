package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.KeywordEmail;

public interface IKeywordEmailService {
	List<KeywordEmail> selectByExample(Criteria criteria);

	void saveKeywordEmail(KeywordEmail keywordEmail);
	
}
