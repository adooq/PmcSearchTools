package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.KeywordEmailMapper;
import selleck.email.pojo.KeywordEmail;
import selleck.email.service.IKeywordEmailService;

public class KeywordEmailServiceImpl implements IKeywordEmailService{
	private String db;
	
	public KeywordEmailServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public List<KeywordEmail> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<KeywordEmail> retun = new ArrayList<KeywordEmail>();
		try{
			KeywordEmailMapper mapper = session.getMapper(KeywordEmailMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveKeywordEmail(KeywordEmail keywordEmail) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			KeywordEmailMapper mapper = session.getMapper(KeywordEmailMapper.class);
			mapper.saveKeywordEmail(keywordEmail);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}


}
