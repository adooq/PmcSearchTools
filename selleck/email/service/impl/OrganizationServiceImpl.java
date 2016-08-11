package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.OrganizationMapper;
import selleck.email.pojo.Organization;
import selleck.email.service.IOrganizationService;

public class OrganizationServiceImpl implements IOrganizationService {
	private String db;

	
	public OrganizationServiceImpl(String db) {
		this.db = db;
	}
	
	@Override
	public List<Organization> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Organization> organizations = new ArrayList<Organization>();
		try {
			OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);
			if (mapper != null) {
				organizations = mapper.selectByExample(criteria);
			} else {
				organizations = null;
			}
		} finally {
			session.close();
		}

		return organizations;
	}

	@Override
	public void updateOrganization(Organization organization) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);
			if (mapper != null) {
				mapper.updateOrganization(organization);
			}
		} finally {
			session.close();
		}
		
	}

	@Override
	public void insertOrganization(Organization organization) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);
			if (mapper != null) {
				mapper.insertOrganization(organization);
			}
		} finally {
			session.close();
		}
		
	}

	@Override
	public void saveOrganization(Organization organization) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);
			if (mapper != null) {
				mapper.saveOrganization(organization);
			}
		} finally {
			session.close();
		}
		
	}

	@Override
	public void deleteOrganization(Organization organ) {
		SqlSession session = MybatisFactory.getSession(db);
		try {
			OrganizationMapper mapper = session.getMapper(OrganizationMapper.class);
			if (mapper != null) {
				mapper.deleteOrganization(organ);
			}
		} finally {
			session.close();
		}
		
	}

}
