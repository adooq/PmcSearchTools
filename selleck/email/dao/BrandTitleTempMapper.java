package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.BrandTitleTemp;

public interface BrandTitleTempMapper {
	void insert(BrandTitleTemp brandTitleTemp);
	List<BrandTitleTemp> selectByCriteria(Criteria criteria);
	void setRead(BrandTitleTemp brandTitleTemp);
	int selectMaxId();
}
