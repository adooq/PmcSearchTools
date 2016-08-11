package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.LabmemReferenceMapper;
import selleck.email.pojo.LabmemReference;
import selleck.email.service.IOrganitionEmailService;

public class OrganitionEmailServiceImpl implements IOrganitionEmailService{

	@Override
	public List<LabmemReference> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<LabmemReference> retun = new ArrayList<LabmemReference>();
		try{
			LabmemReferenceMapper mapper = session.getMapper(LabmemReferenceMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<LabmemReference> selectLabUrlNotNull(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<LabmemReference> retun = new ArrayList<LabmemReference>();
		try{
			LabmemReferenceMapper mapper = session.getMapper(LabmemReferenceMapper.class);
			if(mapper!=null){
				retun = mapper.selectLabUrlNotNull(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
}
