package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.KeywordEmail;

public interface KeywordEmailMapper {	
	List<KeywordEmail> selectByExample(Criteria criteria);
	void saveKeywordEmail(KeywordEmail keywordEmail);
}