package selleck.email.controller;

import selleck.email.service.IArticleService;
import selleck.email.service.IAuthorService;
import selleck.email.service.IImportService;
import selleck.email.service.impl.ArticleServiceImpl;
import selleck.email.service.impl.AuthorServiceImpl;
import selleck.email.service.impl.ImportServiceImpl;

public abstract class AbstractParser {
	static String newEmailTableName;
	static String DB; // 进行操作的数据库
	static int START_INDEX; // search_wos_by_publication表起始id
	static int MAX_ID; // search_wos_by_publication表最后的id，通常是max(id) from search_wos_by_publication
	static int STEP; // 一次查询出来的数量
	private static int articleId; // 起始文章id,  > articleId
	private static int authorId; // 起始作者id,  > authorId
	
	final void allProcess(){
		System.out.println("new table name: "+newEmailTableName);
		selectFromId(); // 查询导入总表前最大的id，作为新导入数据起始id
		importEDMDB(); // 导入selleck_edm_article,selleck_edm_author
		changeEmail(); // 修改email
		createNewEmailTable(); // 创建新获得的email的表
	}
	
	/**
	 * 创建新获得的email表
	 */
	final void createNewEmailTable(){
		IImportService importService = new ImportServiceImpl(DB);
		importService.importNewEmails(newEmailTableName,articleId, authorId);
//		
//		importService.createNewEmailTable(newEmailTableName); // 创建表
//		
//		importService.insertNewEmail(newEmailTableName, authorId); // 插入数据
//		
//		int articleCount = importService.selectArticleCount(articleId); // 新文章数
//		System.out.println("文章数: "+articleCount);
//		
//		int emailCount = importService.selectEmailCount(authorId); // 邮箱数
//		System.out.println("获得的Email数: "+emailCount);
//		
//		int newEmailCount = importService.selectNewEmailCount(newEmailTableName); // 新邮箱数
//		System.out.println("新获得的Email数: " + newEmailCount);
	}
	
	/**
	 * 查询selleck_edm_author,selleck_edm_article表的id，以备后用。
	 */
	final void selectFromId(){
		IAuthorService authorService = new AuthorServiceImpl(DB);
		authorId = authorService.selectMaxId();
		System.out.println("max author id: "+authorId);
		IArticleService articleService = new ArticleServiceImpl(DB);
		articleId = articleService.selectMaxId();
		System.out.println("max article id: "+articleId);
	}
	
	/**
	 * 有很多邮箱域名会改变（整体迁移），修改导入selleck_edm_author的邮箱。
	 * 具体SQL 参照emailhunter.Queries.EmailChanges，请保持两者一致。
	 */
	final void changeEmail(){
		IAuthorService authorService = new AuthorServiceImpl(DB);
		authorService.changeEmails(authorId);
	}
	
	abstract void importEDMDB();
}
