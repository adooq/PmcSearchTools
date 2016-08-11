package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.YellowPageMapper;
import selleck.email.pojo.YellowPage;
import selleck.email.service.IYellowPageService;

public class YellowPageServiceImpl implements IYellowPageService {

	@Override
	public List<YellowPage> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession();
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<YellowPage> retun = new ArrayList<YellowPage>();
		try{
			YellowPageMapper mapper = session.getMapper(YellowPageMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}


	@Override
	public void saveYellowPage(YellowPage yp) {
		SqlSession session = MybatisFactory.getSession();
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			YellowPageMapper mapper = session.getMapper(YellowPageMapper.class);
			if (mapper != null) {
				mapper.saveYellowPage(yp);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public void updateYellowPage(YellowPage yp) {
		SqlSession session = MybatisFactory.getSession();
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			YellowPageMapper mapper = session.getMapper(YellowPageMapper.class);
			if (mapper != null) {
				mapper.updateYellowPage(yp);
			}
		} finally {
			session.close();
		}
	}

}
