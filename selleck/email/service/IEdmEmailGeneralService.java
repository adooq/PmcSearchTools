package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.EdmEmailGeneral;

public interface IEdmEmailGeneralService {
	public List<EdmEmailGeneral> selectByExample(Criteria criteria);
	
	public void insert(EdmEmailGeneral email);

	public void updateByPrimaryKey(EdmEmailGeneral email);
}
