package selleck.email.service;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.LabmemReference;

public interface IOrganitionEmailService {
	public List<LabmemReference> selectByExample(Criteria criteria);
	public List<LabmemReference> selectLabUrlNotNull(Criteria criteria);

}
