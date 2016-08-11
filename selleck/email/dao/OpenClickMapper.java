package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.EmailTask;
import selleck.email.pojo.OpenClick;

public interface OpenClickMapper {
	List<OpenClick> selectByExample(Criteria criteria);
	void insertOpen(OpenClick openClick);
	void insertClick(OpenClick openClick);
	void insertHardBounce(String email);
	void insertSoftBounce(String email);
	void insertUnsubscribe(String email);
	void insertUnopen(String email);
	void updateEmailTasks(EmailTask emailTask);
}