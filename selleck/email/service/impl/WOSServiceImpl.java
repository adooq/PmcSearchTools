package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.config.mybatis.MybatisFactory252NotUseDB;
import selleck.email.dao.BrandTitleTempMapper;
import selleck.email.dao.WOSMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.BrandTitleTemp;
import selleck.email.pojo.NewEmail;
import selleck.email.pojo.WOS;
import selleck.email.service.IWOSService;

public class WOSServiceImpl implements IWOSService{
	private String db;
	
	public WOSServiceImpl(String db){
		this.db = db;
	}
	
	@Override
	public List<WOS> selectByExample(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<WOS> retun = new ArrayList<WOS>();
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if(mapper!=null){
				retun = mapper.selectByExample(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList,WOS wos) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(wos);
			
			transaction.commit();
		}catch(Exception e){
			try {
				transaction.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			// e.printStackTrace();
		}finally{
			session.close();
		}
	}

	@Override
	public WOS selectWOSByNewEmail(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		WOS wos = null;
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if(mapper!=null){
				wos = mapper.selectByEmail(criteria);
			}else{
				wos = null;
			}
		}finally{session.close();}
		
		return wos;
		
	}

	@Override
	public List<String> selectNewEmail(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		 List<String> emails = null;
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if(mapper!=null){
				emails = mapper.selectNewEmail(criteria);
			}else{
				emails = null;
			}
		}finally{session.close();}
		
		return emails;
	}

	@Override
	public void updateNewEmail(NewEmail newEmail) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			mapper.updateNewEmail(newEmail);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}

	@Override
	public List<WOS> selectFromSearchRecord(Criteria criteria) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		List<WOS> retun = new ArrayList<WOS>();
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if(mapper!=null){
				retun = mapper.selectFromSearchRecord(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public String findAddressByAuthorName(Criteria criteria) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		String retun = "";
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if(mapper!=null){
				retun = mapper.findAddressByAuthorName(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}
	
	@Override
	public void saveArticleAndAuthorFromSearchRecord(Article article, List<Author> authorList,WOS wos) {
		SqlSession session = MybatisFactory252NotUseDB.getSession();
		Transaction transaction = MybatisFactory252NotUseDB.beginTransaction(session);
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			mapper.insertArticleNotUseDB(article);
			for(Author author : authorList){
				mapper.insertAuthorNotUseDB(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRelNotUseDB(rel);
			}
			mapper.setSearchRecordRead(wos);
			
			transaction.commit();
		}catch(Exception e){
			try {
				transaction.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			// e.printStackTrace();
		}finally{
			session.close();
		}
	}

	@Override
	public void saveWOS(WOS wos) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			mapper.saveWOS(wos);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		
	}
	
	@Override
	public void saveWOSByKeyword(WOS wos,BrandTitleTemp brandTitleTemp) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
		try{
			WOSMapper mapper = session.getMapper(WOSMapper.class);
			if (mapper != null) {
				mapper.saveWOSByKeyword(wos);
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
