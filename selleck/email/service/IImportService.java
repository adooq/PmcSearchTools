package selleck.email.service;

/**
 * 把抓到的email导入email总表要用的一些sql
 * @author fscai
 *
 */
public interface IImportService {
	public void createNewEmailTable(String tableName);
	public void insertNewEmail(String newEmailTableName, int startId);
	public int selectArticleCount(int startId);
	public int selectEmailCount(int startId);
	public int selectNewEmailCount(String newEmailTableName);
	public void importNewEmails(String tableName,  int articleStartId, int authorStartId);
	public void insertIntoCA(String tableName);
	public void insertIntoALL(String tableName);
}
