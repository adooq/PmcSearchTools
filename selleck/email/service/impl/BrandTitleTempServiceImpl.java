package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.service.IBrandTitleTempService;
import selleck.utils.Constants;

public class BrandTitleTempServiceImpl implements IBrandTitleTempService {

	private String db;
	private SqlSession session;

	public BrandTitleTempServiceImpl() {
		this.db = Constants.LIFE_SCIENCE_DB;
	}

	public BrandTitleTempServiceImpl(String db) {
		this.db = db;
	}

	@Override
	public void insert(BrandTitleTemp brandTitleTemp) {
		session = MybatisFactory.getSession(db);
		try {
			BrandTitleTempMapper mapper = session.getMapper(BrandTitleTempMapper.class);
			if (mapper != null) {
				mapper.insert(brandTitleTemp);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public List<BrandTitleTemp> selectByCriteria(Criteria criteria) {
		session = MybatisFactory.getSession(db);
		List<BrandTitleTemp> btts = new ArrayList<BrandTitleTemp>();
		try {
			BrandTitleTempMapper mapper = session.getMapper(BrandTitleTempMapper.class);
			if (mapper != null) {
				btts = mapper.selectByCriteria(criteria);
				for (BrandTitleTemp btt : btts){
					setRead(btt);
				}
			} else {
				btts = null;
			}
		} finally {
			session.close();
		}

		return btts;
	}

	@Override
	public int selectMaxId() {
		session = MybatisFactory.getSession(db);
		int rs = 0;
		try {
			BrandTitleTempMapper mapper = session.getMapper(BrandTitleTempMapper.class);
			if (mapper != null) {
				rs = mapper.selectMaxId();
			}
		} finally {
			session.close();
		}

		return rs;
	}

	@Override
	public void setRead(BrandTitleTemp brandTitleTemp) {
		session = MybatisFactory.getSession(db);
		try {
			BrandTitleTempMapper mapper = session.getMapper(BrandTitleTempMapper.class);
			if (mapper != null) {
				mapper.setRead(brandTitleTemp);
			}
		} finally {
			session.close();
		}
	}

}
