package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.interest.beans.DictClass;

public interface IDictService {
	List<DictClass> selectByExample(Criteria criteria);
	void insertDict(DictClass dc);
}
