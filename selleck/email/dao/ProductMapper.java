package selleck.email.dao;

import java.util.List;

import common.handle.model.Criteria;
import selleck.email.interest.beans.ProductClass;
import selleck.email.interest.beans.RelClass;

public interface ProductMapper {
	List<RelClass> getSmallBigRel();
	List<RelClass> getProductCategoryRel();
	List<ProductClass> getProducts();
	List<ProductClass> getProductsByCriteria(Criteria criteria);
	List<ProductClass> getCategory(Criteria criteria);
}
