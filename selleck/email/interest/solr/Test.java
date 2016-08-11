package selleck.email.interest.solr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

public class Test {
	public static final String SOLR_URL = "http://localhost:7520/solr";
	
	// query parameters
	public static final String TITLE = "title_en_wos";
	public static final String ABSTRACT = "abstract_en_wos";
	public static final String AUTHORS = "authors_en_wos";
	public static final String KEYWORD = "keyword_en_wos";
	public static final String PUBLICATION = "*";
	
	
	public static final String QUERY = "abstract_en_wos:cell";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SolrServer server = new HttpSolrServer(SOLR_URL);
        SolrQuery query = new SolrQuery();  
        query.setQuery(QUERY);
        query.addSort(new SolrQuery.SortClause("score", SolrQuery.ORDER.desc));  
        QueryResponse rsp;
		try {
			rsp = server.query(query);
			List<Article> beans = rsp.getBeans(Article.class);
			if(!beans.isEmpty()){
				File csv = new File("e://solr record.csv"); // CSV文件
				if(csv.exists()){
					csv.delete();
				}	
				BufferedWriter bw = null;
	            try {
	            	csv.createNewFile();
					bw = new BufferedWriter(new FileWriter(csv, true));
					for(Article article : beans){
						bw.write(article.getTitle());
						// bw.write(article.getEmail());
						bw.newLine();
					}
		            bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					if(bw != null){
						try {
							bw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
	            
			}else{
				System.out.println("no record found");
			}
	       
		} catch (SolrServerException e) {
			e.printStackTrace();
		}  
        

	}
	
}
