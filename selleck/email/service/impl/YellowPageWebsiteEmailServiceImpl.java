package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.YellowPageWebsiteEmailMapper;
import selleck.email.pojo.YellowPageWebsiteEmail;
import selleck.email.service.IYellowPageWebsiteEmailService;

public class YellowPageWebsiteEmailServiceImpl implements IYellowPageWebsiteEmailService{
	private String db;
	
	public YellowPageWebsiteEmailServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public List<YellowPageWebsiteEmail> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<YellowPageWebsiteEmail> retun = new ArrayList<YellowPageWebsiteEmail>();
		try{
			YellowPageWebsiteEmailMapper mapper = session.getMapper(YellowPageWebsiteEmailMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void updateYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			YellowPageWebsiteEmailMapper mapper = session.getMapper(YellowPageWebsiteEmailMapper.class);
			mapper.updateYellowPageWebsiteEmail(ypwe);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public void saveYellowPageWebsiteEmail(YellowPageWebsiteEmail ypwe) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			YellowPageWebsiteEmailMapper mapper = session.getMapper(YellowPageWebsiteEmailMapper.class);
			mapper.saveYellowPageWebsiteEmail(ypwe);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}


}
