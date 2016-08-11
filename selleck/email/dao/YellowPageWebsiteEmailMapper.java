package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPageWebsiteEmail;

public interface YellowPageWebsiteEmailMapper {
	List<YellowPageWebsiteEmail> selectByExample(Criteria criteria);
	
	void saveYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe);
	
	void updateYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe);
}
