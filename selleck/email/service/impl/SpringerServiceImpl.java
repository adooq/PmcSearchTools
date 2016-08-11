package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.dao.SpringerMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.Springer;
import selleck.email.service.ISpringerService;

public class SpringerServiceImpl implements ISpringerService {
	private String db;
	
	public SpringerServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public List<Springer> selectBySearchPublication(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<Springer> retun = new ArrayList<Springer>();
		try{
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchPublication(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<Springer> selectBySearchRecord(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<Springer> retun = new ArrayList<Springer>();
		try{
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchRecord(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList, Springer springer) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(springer);
			
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
		public void saveArticleAndAuthorForSearchRecord(Article article, List<Author> authorList, Springer springer) {
			SqlSession session = MybatisFactory.getSession(db);
			Transaction transaction = MybatisFactory.beginTransaction(session);
//			SqlSession session = MybatisFactoryLocal.getSession();
//			Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
			try{
				SpringerMapper mapper = session.getMapper(SpringerMapper.class);
				mapper.insertArticle(article);
				for(Author author : authorList){
					mapper.insertAuthor(author);
					ArticleAuthorRel rel = new ArticleAuthorRel();
					rel.setArticleId(article.getId());
					rel.setAuthorId(author.getId());
					rel.setPriority(author.getPriority());
					mapper.insertArticleAuthorRel(rel);
				}
				mapper.setSearchRecordRead(springer);
				
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
	public void saveSpringer(Springer springer) {
		SqlSession session = null;
		try {
			session = MybatisFactory.getSession(db);
			// SqlSession session = MybatisFactoryLocal.getSession();
			
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			if (mapper != null) {
				mapper.saveSpringer(springer);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			if(session != null){
				session.close();
			}	
		}

	}

	@Override
	public void updateSpringer(Springer springer) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			if (mapper != null) {
				mapper.updateSpringer(springer);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void saveSpringerByKeyword(Springer springer,
			BrandTitleTemp brandTitleTemp) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			SpringerMapper mapper = session.getMapper(SpringerMapper.class);
			if (mapper != null) {
				mapper.saveSpringerByKeyword(springer);
			}
			BrandTitleTempMapper bttMapper = session.getMapper(BrandTitleTempMapper.class);
			bttMapper.setRead(brandTitleTemp);
			
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

}
