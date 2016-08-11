package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.dao.PMCMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.PMC;
import selleck.email.service.IPMCService;

public class PMCServiceImpl implements IPMCService {
	private String db;
	
	public PMCServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<PMC> selectBySearchPublication(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<PMC> retun = new ArrayList<PMC>();
		try{
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchPublication(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<PMC> selectBySearchRecord(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<PMC> retun = new ArrayList<PMC>();
		try{
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchRecord(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList, PMC pmc) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(pmc);
			
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
	public void saveArticleAndAuthorForSearchRecord(Article article, List<Author> authorList, PMC pmc) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setSearchRecordRead(pmc);
			
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
	public void savePMC(PMC pmc) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			if (mapper != null) {
				mapper.savePMC(pmc);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public void updatePMC(PMC pmc) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			if (mapper != null) {
				mapper.updatePMC(pmc);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void savePMCByKeyword(PMC pmc, BrandTitleTemp brandTitleTemp) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			PMCMapper mapper = session.getMapper(PMCMapper.class);
			if (mapper != null) {
				mapper.savePMCByKeyword(pmc);
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
