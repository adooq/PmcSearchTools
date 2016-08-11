package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.DXY_Reply;
import selleck.email.pojo.DXY_Topic;

public interface IDXYService {
	List<DXY_Topic> selectTopic(Criteria criteria);
	List<DXY_Reply> selectReply(Criteria criteria);
	void saveTopic(DXY_Topic topic);
	void saveReply(DXY_Reply reply);
	void updateTopic(DXY_Topic topic);
	void updateReply(DXY_Reply reply);
}
