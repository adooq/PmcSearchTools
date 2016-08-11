package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.OpenClickMapper;
import selleck.email.pojo.EmailTask;
import selleck.email.pojo.OpenClick;
import selleck.email.service.IOpenClickService;

public class OpenClickServiceImpl implements IOpenClickService{
	private String db;
	
	public OpenClickServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<OpenClick> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<OpenClick> retun = new ArrayList<OpenClick>();
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void insertOpen(OpenClick openClick) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertOpen(openClick);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}
	
	@Override
	public void insertClick(OpenClick openClick) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertClick(openClick);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public void insertHardBounce(String email) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertHardBounce(email);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public void insertSoftBounce(String email) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertSoftBounce(email);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public void insertUnsubscribe(String email) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertUnsubscribe(email);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}
	
	@Override
	public void insertUnopen(String email) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.insertUnopen(email);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public void updateEmailTasks(EmailTask emailTask) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			OpenClickMapper mapper = session.getMapper(OpenClickMapper.class);
			if(mapper!=null){
				mapper.updateEmailTasks(emailTask);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}

	}
}
