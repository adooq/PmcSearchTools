package selleck.email.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;

import common.handle.model.Criteria;
import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.PubmedMapper;
import selleck.email.pojo.Article;
import selleck.email.pojo.ArticleAuthorRel;
import selleck.email.pojo.Author;
import selleck.email.pojo.Pubmed;
import selleck.email.service.IPubmedService;

public class PubmedServiceImpl implements IPubmedService {
	private String db;
	
	public PubmedServiceImpl(String db){
		this.db = db;
	}

	@Override
	public List<Pubmed> selectBySearchPublication(Criteria criteria) {
		SqlSession session = MybatisFactory.getSession(db);
		List<Pubmed> retun = new ArrayList<Pubmed>();
		try{
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			if(mapper!=null){
				retun = mapper.selectBySearchPublication(criteria);
			}else{
				retun = null;
			}
		}finally{session.close();}
		
		return retun;
	}

	@Override
	public void saveArticleAndAuthor(Article article, List<Author> authorList, Pubmed pubmed) {
		SqlSession session = MybatisFactory.getSession(db);
		Transaction transaction = MybatisFactory.beginTransaction(session);
//		SqlSession session = MybatisFactoryLocal.getSession();
//		Transaction transaction = MybatisFactoryLocal.beginTransaction(session);
		try{
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			mapper.insertArticle(article);
			for(Author author : authorList){
				mapper.insertAuthor(author);
				ArticleAuthorRel rel = new ArticleAuthorRel();
				rel.setArticleId(article.getId());
				rel.setAuthorId(author.getId());
				rel.setPriority(author.getPriority());
				mapper.insertArticleAuthorRel(rel);
			}
			mapper.setRead(pubmed);
			
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
	public void savePubmed(Pubmed pubmed) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			if (mapper != null) {
				mapper.savePubmed(pubmed);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

	}

	@Override
	public void updatePubmed(Pubmed pubmed) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			if (mapper != null) {
				mapper.updatePubmed(pubmed);
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			session.close();
		}
	}
	
	@Override
	public void savePubmedByKeyword(Pubmed pubmed) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			if (mapper != null) {
				mapper.savePubmedByKeyword(pubmed);
			}
		} catch (Exception e){
			e.printStackTrace();
		}finally {
			session.close();
		}

	}

	@Override
	public void updatePubmedByKeyword(Pubmed pubmed) {
		SqlSession session = MybatisFactory.getSession(db);
		// SqlSession session = MybatisFactoryLocal.getSession();
		try {
			PubmedMapper mapper = session.getMapper(PubmedMapper.class);
			if (mapper != null) {
				mapper.updatePubmedByKeyword(pubmed);
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			session.close();
		}
	}

}
