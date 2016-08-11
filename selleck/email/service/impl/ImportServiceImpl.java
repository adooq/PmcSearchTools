package selleck.email.service.impl;

import org.apache.ibatis.session.SqlSession;

import selleck.config.mybatis.MybatisFactory;
import selleck.email.dao.ImportMapper;
import selleck.email.service.IImportService;
import selleck.utils.Constants;

public class ImportServiceImpl implements IImportService {
	private String db;
	private SqlSession session;

	public ImportServiceImpl(){
		this.db = Constants.LIFE_SCIENCE_DB;
	}
	
	public ImportServiceImpl(String db){
		this.db = db;
	}
	
	/**
	 * 把新获得的email单独导出一张表，并显示数量。
	 * @param tableName 新建的表名
	 * @param articleStartId 
	 * @param authorStartId
	 */
	public void importNewEmails(String tableName,  int articleStartId, int authorStartId) {
		session = MybatisFactory.getSession(db);
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				mapper.createNewEmailTable(tableName);
				mapper.insertNewEmail(tableName, authorStartId);
				System.out.println("文章数: "+mapper.selectArticleCount(articleStartId));
				System.out.println("获得的Email数: "+mapper.selectEmailCount(authorStartId));
				System.out.println("新获得的Email数: " + mapper.selectNewEmailCount(tableName));
				mapper.insertIntoCA(tableName);
				System.out.println("inserted into author_ca");
				mapper.insertIntoALL(tableName);
				System.out.println("inserted into author");
			}
		}finally{session.close();}
		
	}
	
	@Override
	public void createNewEmailTable(String tableName) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				mapper.createNewEmailTable(tableName);
			}
		}finally{session.close();}
		
	}

	@Override
	public void insertNewEmail(String newEmailTableName, int startId) {
		SqlSession session = MybatisFactory.getSession(db);
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				mapper.insertNewEmail(newEmailTableName, startId);
			}
		}finally{session.close();}
		
	}

	@Override
	public int selectArticleCount(int startId) {
		SqlSession session = MybatisFactory.getSession(db);
		int rs = 0;
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				rs = mapper.selectArticleCount(startId);
			}
		}finally{session.close();}
		return rs;
	}

	@Override
	public int selectEmailCount(int startId) {
		SqlSession session = MybatisFactory.getSession(db);
		int rs = 0;
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				rs = mapper.selectEmailCount(startId);
			}
		}finally{session.close();}
		return rs;
	}

	@Override
	public int selectNewEmailCount(String newEmailTableName) {
		SqlSession session = MybatisFactory.getSession(db);
		int rs = 0;
		try{
			ImportMapper mapper = session.getMapper(ImportMapper.class);
			if(mapper!=null){
				rs = mapper.selectNewEmailCount(newEmailTableName);
			}
		}finally{session.close();}
		return rs;
	}

	@Override
	public void insertIntoCA(String tableName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insertIntoALL(String tableName) {
		// TODO Auto-generated method stub
		
	}

}
