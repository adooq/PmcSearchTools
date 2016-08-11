package selleck.config.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import selleck.utils.Constants;

public class MybatisFactory {
    private static SqlSessionFactory sqlSessionFactory = null;  


    /** 
     * @throws IOException 
     */  
    private static void initialFactory(String db , Properties prop) throws IOException {
    	String resource = "";
    	if(db.equals(Constants.LIFE_SCIENCE_DB)){
    		resource = "selleck/config/mybatis/mybatisConfig252LifeScience.xml";  
		}else if (db.equals(Constants.MATERIAL_SCIENCE_DB)){
			resource = "selleck/config/mybatis/mybatisConfig252MaterialScience.xml";  
		}else if (db.equals(Constants.CHEMISTRY_DB)){
			resource = "selleck/config/mybatis/mybatisConfig252Chemistry.xml";
		}else if (db.equals(Constants.LOCAL)){
			resource = "selleck/config/mybatis/mybatisConfigLocal.xml";
		}else if (db.equals(Constants.JUN)){
			resource = "selleck/config/mybatis/mybatisConfigLocalForJun.xml";
		}else if (db.equals(Constants.DYNAMIC)){
			resource = "selleck/config/mybatis/mybatisConfigDynamic.xml";
		}else if (db.equals(Constants.LOCAL239)){
			resource = "selleck/config/mybatis/mybatisConfig239.xml";
		}
        InputStream inputStream = Resources.getResourceAsStream(resource);
        if(prop == null){
        	try{
        		sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }else{
        	sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,prop);
        }
    }
    
    public static SqlSession getSession() {
        return  getSession(Constants.LIFE_SCIENCE_DB , null);  
    }

    public static SqlSession getSession(String dbName) {  
        return  getSession(dbName , null);  
    }
    
    public static SqlSession getSession(String dbName , Properties prop) {  
        if(sqlSessionFactory == null) {  
            try {  
                initialFactory(dbName,prop);  
            } catch (IOException e) {
                e.printStackTrace();
            }  
        }  
        return  sqlSessionFactory.openSession(true);  
    }
    
    /**
     * @param sqlSession
     * @return
     */
    public static Transaction beginTransaction(SqlSession sqlSession){
    	TransactionFactory transactionFactory = new JdbcTransactionFactory();
    	try {
			sqlSession.getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return transactionFactory.newTransaction(sqlSession.getConnection());
    }
}
