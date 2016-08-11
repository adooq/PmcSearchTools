package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.CNKIMapper;
import selleck.email.pojo.CNKI;
import selleck.email.service.ICNKIService;

public class CNKIServiceImpl implements ICNKIService{

	@Override
	public List<CNKI> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<CNKI> retun = new ArrayList<CNKI>();
		try{
			CNKIMapper mapper = session.getMapper(CNKIMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<CNKI> selectForWangguo(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<CNKI> retun = new ArrayList<CNKI>();
		try{
			CNKIMapper mapper = session.getMapper(CNKIMapper.class);
			if(mapper!=null){
				retun = mapper.selectForWangguo(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void updateEmailAndEnAbs(CNKI cnki) {
		SqlSession session = MybatisFactory.getSession();
		try{
			CNKIMapper mapper = session.getMapper(CNKIMapper.class);
			if(mapper!=null){
				 mapper.updateEmailAndEnAbs(cnki);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally{session.close();}
		
	}

}
