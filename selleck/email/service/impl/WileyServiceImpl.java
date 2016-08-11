package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.dao.WileyMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.Wiley;
import selleck.email.service.IWileyService;

public class WileyServiceImpl implements IWileyService {
	private String db;
	
	public WileyServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<Wiley> selectFromSearchPublication(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<Wiley> retun = new ArrayList<Wiley>();
		try{
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			if(mapper!=null){
				retun = mapper.selectFromSearchPublication(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public List<Wiley> selectFromSearchRecord(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		List<Wiley> retun = new ArrayList<Wiley>();
		try{
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			if(mapper!=null){
				retun = mapper.selectFromSearchRecord(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList, Wiley wiley) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(wiley);
			
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
	public void saveArticleAndAuthorForSearchRecord(Article article, List<Author> authorList, Wiley wiley) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setSearchRecordRead(wiley);
			
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
	public void saveWiley(Wiley wiley) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			if (mapper != null) {
				mapper.saveWiley(wiley);
			}
		} finally {
			session.close();
		}

	}

	@Override
	public void updateWiley(Wiley wiley) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			if (mapper != null) {
				mapper.updateWiley(wiley);
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void saveWileyByKeyword(Wiley wiley, BrandTitleTemp brandTitleTemp) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			WileyMapper mapper = session.getMapper(WileyMapper.class);
			if (mapper != null) {
				mapper.saveWileyByKeyword(wiley);
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
