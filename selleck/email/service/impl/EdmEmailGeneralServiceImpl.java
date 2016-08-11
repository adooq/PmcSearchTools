package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.EdmEmailGeneralMapper;
import selleck.email.pojo.EdmEmailGeneral;
import selleck.email.service.IEdmEmailGeneralService;

public class EdmEmailGeneralServiceImpl implements IEdmEmailGeneralService{

	@Override
	public void insert(EdmEmailGeneral email) {
		// TODO Auto-generated method stub
		SqlSession session = MybatisFactory.getSession();
		try{
			EdmEmailGeneralMapper mapper = session.getMapper(EdmEmailGeneralMapper.class);
			//session.getConnection().setAutoCommit(false);
			if(mapper!=null){
				mapper.insert(email);
			}
			//session.getConnection().commit();
		}catch(Exception e){
			//try{session.getConnection().rollback();}catch(Exception e2){}
			e.printStackTrace();
		}finally{
			session.close();
		}
	}
	
	
	@Override
	public void updateByPrimaryKey(EdmEmailGeneral email) {
		// TODO Auto-generated method stub
		SqlSession session = MybatisFactory.getSession();
		try{
			EdmEmailGeneralMapper mapper = session.getMapper(EdmEmailGeneralMapper.class);
			//session.getConnection().setAutoCommit(false);
			if(mapper!=null){
				mapper.updateByPrimaryKey(email);
			}
			//session.getConnection().commit();
		}catch(Exception e){
			//try{session.getConnection().rollback();}catch(Exception e2){}
			e.printStackTrace();
		}finally{
			session.close();
		}
	}
	
	@Override
	public List<EdmEmailGeneral> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		List<EdmEmailGeneral> retun = new ArrayList<EdmEmailGeneral>();
		try{
			EdmEmailGeneralMapper mapper = session.getMapper(EdmEmailGeneralMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}catch(Exception e){
			//try{session.getConnection().rollback();}catch(Exception e2){}
			e.printStackTrace();
		} finally{session.close();}
		
		return retun;
	}
}
