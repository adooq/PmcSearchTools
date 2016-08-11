package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.CNKI;

public interface CNKIMapper {
	public List<CNKI> selectByExample(Criteria criteria);
	public List<CNKI> selectForWangguo(Criteria criteria);
	public void updateEmailAndEnAbs(CNKI cnki);
}