package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.DictMapper;
import selleck.email.interest.beans.DictClass;
import selleck.email.service.IDictService;

public class DictServiceImpl implements IDictService{

	@Override
	public List<DictClass> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<DictClass> retun = new ArrayList<DictClass>();
		try{
			DictMapper mapper = session.getMapper(DictMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void insertDict(DictClass dc) {
		SqlSession session = MybatisFactory.getSession();
		try{
			DictMapper mapper = session.getMapper(DictMapper.class);
			if(mapper!=null){
				mapper.insertDict(dc);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

}
