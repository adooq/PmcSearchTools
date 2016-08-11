package selleck.email.dao;

public interface ImportMapper {
	public void createNewEmailTable(String tableName);
	public void insertNewEmail(String newEmailTableName, int startId);
	public int selectArticleCount(int startId);
	public int selectEmailCount(int startId);
	public int selectNewEmailCount(String newEmailTableName);
	public void insertIntoCA(String tableName);
	public void insertIntoALL(String tableName);
}
