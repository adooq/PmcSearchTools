package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory252NotUseDB;
import selleck.email.dao.GoogleSearchMapper;
import selleck.email.pojo.GoogleSearch;
import selleck.email.service.IGoogleSearchService;

public class GoogleSearchServiceImpl implements IGoogleSearchService {

	@Override
	public List<GoogleSearch> selectByCriteria(Criteria criteria) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<GoogleSearch> list = new ArrayList<GoogleSearch>();
		try {
			GoogleSearchMapper mapper = session
					.getMapper(GoogleSearchMapper.class);
			if (mapper != null) {
				list = mapper.selectByCriteria(criteria);
			} else {
				list = null;
			}
		} finally {
			session.close();
		}

		return list;
	}

	@Override
	public List<GoogleSearch> selectDup(Criteria criteria) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<GoogleSearch> list = new ArrayList<GoogleSearch>();
		try {
			GoogleSearchMapper mapper = session
					.getMapper(GoogleSearchMapper.class);
			if (mapper != null) {
				list = mapper.selectDup(criteria);
			} else {
				list = null;
			}
		} finally {
			session.close();
		}

		return list;
	}

	@Override
	public void updateGoogleSearch(GoogleSearch googleSearch) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleSearchMapper mapper = session
					.getMapper(GoogleSearchMapper.class);
			mapper.updateGoogleSearch(googleSearch);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}

	}

	@Override
	public List<GoogleSearch> selectGSEmailByCriteria(Criteria criteria) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<GoogleSearch> list = new ArrayList<GoogleSearch>();
		try {
			GoogleSearchMapper mapper = session
					.getMapper(GoogleSearchMapper.class);
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
	public void updateGoogleSearchEmail(GoogleSearch googleSearch) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleSearchMapper mapper = session.getMapper(GoogleSearchMapper.class);
			mapper.updateGoogleSearchEmail(googleSearch);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		
	}

}
