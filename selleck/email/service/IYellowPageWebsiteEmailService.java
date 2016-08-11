package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.YellowPageWebsiteEmail;

public interface IYellowPageWebsiteEmailService {
	List<YellowPageWebsiteEmail> selectByExample(Criteria criteria);
	void saveYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe);
	void updateYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe);
}
