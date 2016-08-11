package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.config.mybatis.MybatisFactoryLocal;
import selleck.email.dao.AuthorInterestMapper;
import selleck.email.pojo.AuthorInterest;
import selleck.email.service.IAuthorInterestService;


public class AuthorInterestServiceImpl implements IAuthorInterestService{

	@Override
	public void saveAuthorInterest(AuthorInterest a1, AuthorInterest a2) {
		SqlSession session = MybatisFactory.getSession();
		// SqlSession session = MybatisFactoryLocal.getSession();
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			AuthorInterestMapper mapper = session.getMapper(AuthorInterestMapper.class);
			mapper.saveAuthorInterest(a1);
			if(a2 != null){
				mapper.saveAuthorInterest(a2);
			}
			transaction.commit();
		}catch(Exception e){
			try {
				transaction.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public List<AuthorInterest> getAuthorInterest(Criteria criteria) {
		// SqlSession session = MybatisFactory252.getSession();
		SqlSession session = MybatisFactoryLocal.getSession();
		List<AuthorInterest> retun = new ArrayList<AuthorInterest>();
		try{
			AuthorInterestMapper mapper = session.getMapper(AuthorInterestMapper.class);
			if(mapper!=null){
				retun = mapper.getAuthorInterest(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}



}
