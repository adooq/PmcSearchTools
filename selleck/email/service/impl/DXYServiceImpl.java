package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.DXYMapper;
import selleck.email.pojo.DXY_Reply;
import selleck.email.pojo.DXY_Topic;
import selleck.email.service.IDXYService;

public class DXYServiceImpl implements IDXYService {
	private String db;
	
	public DXYServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<DXY_Topic> selectTopic(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<DXY_Topic> retun = new ArrayList<DXY_Topic>();
		try{
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if(mapper!=null){
				retun = mapper.selectTopic(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<DXY_Reply> selectReply(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<DXY_Reply> retun = new ArrayList<DXY_Reply>();
		try{
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if(mapper!=null){
				retun = mapper.selectReply(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}


	
	@Override
	public void saveTopic(DXY_Topic topic) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if (mapper != null) {
				mapper.saveTopic(topic);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

	}

	@Override
	public void saveReply(DXY_Reply reply) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if (mapper != null) {
				mapper.saveReply(reply);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

	}

	@Override
	public void updateTopic(DXY_Topic topic) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if (mapper != null) {
				mapper.updateTopic(topic);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

		
	}

	@Override
	public void updateReply(DXY_Reply reply) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			DXYMapper mapper = session.getMapper(DXYMapper.class);
			if (mapper != null) {
				mapper.updateReply(reply);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

		
	}

}
