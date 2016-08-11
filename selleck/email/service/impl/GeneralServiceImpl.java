package selleck.email.service.impl;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory252NotUseDB;
import selleck.email.dao.GeneralMapper;
import selleck.email.pojo.AuthorEmailSuffix;
import selleck.email.pojo.OrganizationSuffix;
import selleck.email.service.IGeneralService;

public class GeneralServiceImpl implements IGeneralService {

	@Override
	public List<AuthorEmailSuffix> selectAuthorEmailNoSuffix(Criteria criteria) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<AuthorEmailSuffix>  list = null;
		try{
			GeneralMapper mapper = session.getMapper(GeneralMapper.class);
			if(mapper!=null){
				list = mapper.selectAuthorEmailNoSuffix(criteria);
			}else{
				list = null;
			}
		}finally{session.close();}
		
		return list;
	}

	@Override
	public List<OrganizationSuffix> selectOrganizationSuffix(Criteria criteria) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<OrganizationSuffix>  list = null;
		try{
			GeneralMapper mapper = session.getMapper(GeneralMapper.class);
			if(mapper!=null){
				list = mapper.selectOrganizationSuffix(criteria);
			}else{
				list = null;
			}
		}finally{session.close();}
		
		return list;
	}

	@Override
	public void updateAuthorEmailNoSuffix(AuthorEmailSuffix aes) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try{
			GeneralMapper mapper = session.getMapper(GeneralMapper.class);
			mapper.updateAuthorEmailNoSuffix(aes);
		}finally{session.close();}
		
		
	}

}
