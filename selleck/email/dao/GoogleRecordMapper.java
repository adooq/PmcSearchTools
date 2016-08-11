package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleRecord;
import selleck.email.pojo.GoogleSearchEmail;

public interface GoogleRecordMapper {
	List<GoogleRecord> selectByCriteria(Criteria criteria);
	void updateEmailByGoogleSearchEmail(GoogleSearchEmail gse);
	void insertGoogleRecord(GoogleRecord gr);
	void deleteByGoogleSearchEmail(GoogleSearchEmail gse);
}
