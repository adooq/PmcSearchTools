package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.LabEmailMapper;
import selleck.email.pojo.LabEmail;
import selleck.email.service.ILabEmailService;

public class LabEmailServiceImpl implements ILabEmailService{
	private String db;
	
	public LabEmailServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public void deleteByCriteria(Criteria criteria){
		SqlSession session = MybatisFactory.getSession(db);
		try{
			LabEmailMapper mapper = session.getMapper(LabEmailMapper.class);
			mapper.deleteByCriteria(criteria);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
	}
	
	@Override
	public List<LabEmail> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<LabEmail> retun = new ArrayList<LabEmail>();
		try{
			LabEmailMapper mapper = session.getMapper(LabEmailMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void updateLabEmail(LabEmail labEmail) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			LabEmailMapper mapper = session.getMapper(LabEmailMapper.class);
			mapper.updateLabEmail(labEmail);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
	}

	@Override
	public void saveLabEmail(LabEmail labEmail) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			LabEmailMapper mapper = session.getMapper(LabEmailMapper.class);
			mapper.saveLabEmail(labEmail);
		}catch(Exception e){
			// e.printStackTrace();
		}finally{
			session.close();
		}
		
	}


}
