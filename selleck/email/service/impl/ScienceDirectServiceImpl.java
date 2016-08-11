package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.dao.ScienceDirectMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.ScienceDirect;
import selleck.email.service.IScienceDirectService;

public class ScienceDirectServiceImpl implements IScienceDirectService {
	private String db;
	
	public ScienceDirectServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<ScienceDirect> selectBySearchPublicaton(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<ScienceDirect> retun = new ArrayList<ScienceDirect>();
		try{
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchPublication(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<ScienceDirect> selectBySearchRecord(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<ScienceDirect> retun = new ArrayList<ScienceDirect>();
		try{
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchRecord(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList, ScienceDirect scienceDirect) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(scienceDirect);
			
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
	public void saveArticleAndAuthorForSearchRecord(Article article, List<Author> authorList, ScienceDirect scienceDirect) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setSearchRecordRead(scienceDirect);
			
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
	public void saveScienceDirect(ScienceDirect scienceDirect) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			if (mapper != null) {
				mapper.saveScienceDirect(scienceDirect);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public void updateScienceDirect(ScienceDirect scienceDirect) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			if (mapper != null) {
				mapper.updateScienceDirect(scienceDirect);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void saveScienceDirectByKeyword(ScienceDirect scienceDirect,
			BrandTitleTemp brandTitleTemp) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			ScienceDirectMapper mapper = session.getMapper(ScienceDirectMapper.class);
			if (mapper != null) {
				mapper.saveScienceDirectByKeyword(scienceDirect);
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
