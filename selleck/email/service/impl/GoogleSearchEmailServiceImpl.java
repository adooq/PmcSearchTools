package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory252NotUseDB;
import selleck.config.mybatis.MybatisFactoryLocal;
import selleck.email.dao.GoogleSearchEmailMapper;
import selleck.email.pojo.GoogleSearchEmail;
import selleck.email.service.IGoogleSearchEmailService;

public class GoogleSearchEmailServiceImpl implements IGoogleSearchEmailService {

	@Override
	public List<GoogleSearchEmail> selectGSEmailByCriteria(Criteria criteria) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<GoogleSearchEmail> list = new ArrayList<GoogleSearchEmail>();
		try {
			GoogleSearchEmailMapper mapper = session.getMapper(GoogleSearchEmailMapper.class);
			if (mapper != null) {
				list = mapper.selectGSEmailByCriteria(criteria);
			} else {
				list = null;
			}
		} finally {
			session.close();
		}

		return list;
	}

	@Override
	public void updateFullAuthor(GoogleSearchEmail gse) {
		SqlSession session = MybatisFactoryLocal.getSession();
		// SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleSearchEmailMapper mapper = session.getMapper(GoogleSearchEmailMapper.class);
			mapper.updateFullAuthor(gse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}

	}

	@Override
	public void updateGetEmail(GoogleSearchEmail gse) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleSearchEmailMapper mapper = session.getMapper(GoogleSearchEmailMapper.class);
			mapper.updateGetEmail(gse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}

	}

}
