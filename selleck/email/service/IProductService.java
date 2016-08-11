package selleck.email.service;


import java.util.List;
import java.util.Map;

import common.handle.model.Criteria;
import selleck.email.interest.beans.ProductClass;
import selleck.email.interest.beans.RelClass;

public interface IProductService {
	List<RelClass> getSmallBigRel();
	List<RelClass> getProductCategoryRel();
	Map<Integer, ProductClass> getProducts();
	List< ProductClass> getProductsByCriteria(Criteria criteria);
	List<ProductClass> getCategory(Criteria criteria);
	Map<Integer, ProductClass> getCategoryMap();
}
