package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleRecord;
import selleck.email.pojo.GoogleSearchEmail;

public interface IGoogleRecordService {
	List<GoogleRecord> selectByCriteria(Criteria criteria);
	void updateEmailByGoogleSearchEmail(GoogleSearchEmail gse);
	void insertGoogleRecord(GoogleRecord gr);
	void deleteByGoogleSearchEmail(GoogleSearchEmail gse);
}
