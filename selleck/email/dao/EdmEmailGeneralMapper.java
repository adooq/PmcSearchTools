package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.EdmEmailGeneral;

public interface EdmEmailGeneralMapper {
	
	public void insert(EdmEmailGeneral email);
	
	public List<EdmEmailGeneral> selectByExample(Criteria criteria);
	
	/**
	 * 根据主键更新记录
	 */
	int updateByPrimaryKey(EdmEmailGeneral record);

}