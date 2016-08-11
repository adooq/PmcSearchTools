package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.BrandTitleTemp;


public interface IBrandTitleTempService {
	void insert(BrandTitleTemp brandTitleTemp);
	List<BrandTitleTemp> selectByCriteria(Criteria criteria);
	int selectMaxId();
	void setRead(BrandTitleTemp brandTitleTemp);
}
