package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.pojo.LabmemReference;

public interface LabmemReferenceMapper {
	
	public List<LabmemReference> selectByExample(Criteria criteria);
	
	public void insert(LabmemReference email);
	
	public List<LabmemReference> selectLabUrlNotNull(Criteria criteria);
}