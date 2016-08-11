package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.GoogleSearch;

public interface IGoogleSearchService {
	List<GoogleSearch> selectByCriteria(Criteria criteria);
	List<GoogleSearch> selectDup(Criteria criteria);
	void updateGoogleSearch(GoogleSearch googleSearch);
	
	// google_search_email
	List<GoogleSearch> selectGSEmailByCriteria(Criteria criteria);
	void updateGoogleSearchEmail(GoogleSearch googleSearch);
}
