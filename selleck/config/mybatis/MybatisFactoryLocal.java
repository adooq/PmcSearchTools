package selleck.config.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

@Deprecated
/**
 * 集成到MybatisFactory
 */
public class MybatisFactoryLocal {
    private static SqlSessionFactory sqlSessionFactory = null;  

    /** 
     * ��ʼ��Session���� 
     * @throws IOException 
     */  
    private static void initialFactory() throws IOException {  
        String resource = "selleck/config/mybatis/mybatisConfigLocal.xml";  
        InputStream inputStream = Resources.getResourceAsStream(resource);  
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);  
    }  
    
    /** 
     * ��ȡSession 
     * @return 
     */  
    public static SqlSession getSession() {  
        if(sqlSessionFactory == null) {  
            try {  
                initialFactory();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
        return  sqlSessionFactory.openSession(true);  
    }
    
    /**
     * ��ʼ����
     * @param sqlSession
     * @return
     */
    public static Transaction beginTransaction(SqlSession sqlSession){
    	TransactionFactory transactionFactory = new JdbcTransactionFactory();   //���񹤳�
    	try {
			sqlSession.getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return transactionFactory.newTransaction(sqlSession.getConnection());
    }
}
