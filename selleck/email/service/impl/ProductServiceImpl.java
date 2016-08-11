package selleck.email.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.session.SqlSession;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.ProductMapper;
import selleck.email.interest.beans.ProductClass;
import selleck.email.interest.beans.RelClass;
import selleck.email.service.IProductService;
import selleck.utils.Constants;

public class ProductServiceImpl implements IProductService {
	private String db;
	private Properties prop = null;
	
	public ProductServiceImpl(String db , Properties prop){
		this.db = db;
		this.prop = prop;
	}
	
	public ProductServiceImpl(String db){
		this.db = db;
		this.prop = null;
	}
	
	public ProductServiceImpl(){
		this.db = Constants.LIFE_SCIENCE_DB;
		this.prop = null;
	}

	@Override
	public List<RelClass> getSmallBigRel() {
		// SqlSession session = MybatisFactory252.getSession();
		SqlSession session = MybatisFactory.getSession(db,prop);
		List<RelClass> ret = new ArrayList<RelClass>();
		try{
			ProductMapper mapper = session.getMapper(ProductMapper.class);
			if(mapper!=null){
				ret = mapper.getSmallBigRel();
			}else{
				ret = null;
			}
		}finally{session.close();}
		
		return ret;
	}

	@Override
	public List<RelClass> getProductCategoryRel() {
		// SqlSession session = MybatisFactory252.getSession();
		SqlSession session = MybatisFactory.getSession(db,prop);
		List<RelClass> ret = new ArrayList<RelClass>();
		try{
			ProductMapper mapper = session.getMapper(ProductMapper.class);
			if(mapper!=null){
				ret = mapper.getProductCategoryRel();
			}else{
				ret = null;
			}
		}finally{session.close();}
		
		return ret;
	}

	@Override
	public Map<Integer, ProductClass> getProducts() {
		// SqlSession session = MybatisFactory252.getSession();
		SqlSession session = MybatisFactory.getSession(db,prop);
		Map<Integer, ProductClass> ret = new HashMap<Integer, ProductClass>();
		try{
			ProductMapper mapper = session.getMapper(ProductMapper.class);
			if(mapper!=null){
				List<ProductClass> productList = mapper.getProducts();
				for(ProductClass p : productList){
					ret.put(p.getId(), p);
				}
			}else{
				ret = null;
			}
		}finally{session.close();}
		
		return ret;
	}

	@Override
	public List<ProductClass> getCategory(Criteria criteria) {
		// SqlSession session = MybatisFactory252.getSession();
		SqlSession session = MybatisFactory.getSession(db,prop);
		List<ProductClass> ret = new ArrayList<ProductClass>();
		try{
			ProductMapper mapper = session.getMapper(ProductMapper.class);
			if(mapper!=null){
				ret = mapper.getCategory(criteria);
			}else{
				ret = null;
			}
		}finally{session.close();}
		
		return ret;
	}

	@Override
	public Map<Integer, ProductClass> getCategoryMap() {
		Map<Integer, ProductClass> ret = new HashMap<Integer, ProductClass>();
		Criteria criteria = new Criteria();
		List<ProductClass> productList = getCategory(criteria);
		for(ProductClass p : productList){
			ret.put(p.getId(), p);
		}
		return ret;
	}

	@Override
	public List<ProductClass> getProductsByCriteria(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db,prop);
		List<ProductClass> productList;
		try{
			ProductMapper mapper = session.getMapper(ProductMapper.class);
			if(mapper!=null){
				productList = mapper.getProductsByCriteria(criteria);
			}else{
				productList = null;
			}
		}finally{session.close();}
		
		return productList;
	}

}
