package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory252NotUseDB;
import selleck.email.dao.GoogleRecordMapper;
import selleck.email.pojo.GoogleRecord;
import selleck.email.pojo.GoogleSearchEmail;
import selleck.email.service.IGoogleRecordService;

public class GoogleRecordServiceImpl implements IGoogleRecordService {

	@Override
	public List<GoogleRecord> selectByCriteria(Criteria criteria) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<GoogleRecord> list = new ArrayList<GoogleRecord>();
		try {
			GoogleRecordMapper mapper = session.getMapper(GoogleRecordMapper.class);
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
	public void updateEmailByGoogleSearchEmail(GoogleSearchEmail gse) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleRecordMapper mapper = session.getMapper(GoogleRecordMapper.class);
			mapper.updateEmailByGoogleSearchEmail(gse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		
	}

	@Override
	public void insertGoogleRecord(GoogleRecord gr) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleRecordMapper mapper = session.getMapper(GoogleRecordMapper.class);
			mapper.insertGoogleRecord(gr);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		
	}

	@Override
	public void deleteByGoogleSearchEmail(GoogleSearchEmail gse) {
		// SqlSession session = MybatisFactoryLocal.getSession();
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		try {
			GoogleRecordMapper mapper = session.getMapper(GoogleRecordMapper.class);
			mapper.deleteByGoogleSearchEmail(gse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		
	}
	
}
