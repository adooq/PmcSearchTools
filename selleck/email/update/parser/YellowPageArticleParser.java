package selleck.email.update.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;

import selleck.email.pojo.YellowPage;
import selleck.email.update.tools.ParserUtils;

public class YellowPageArticleParser {
	public static final char CHAR_10 = (char)10; // asc码值是10的字符，是个换行符之类的东西
	public static final String SPACE = "\\p{Space}*"; //  \p{Space}	A whitespace character: [ \t\n\x0B\f\r]
	
	/**
	 * 解析YellowPage页面，抓取YellowPage各个属性
	 * @param htmlStr
	 * @param yellowPage
	 * @return 从页面抓取各个属性后生成的YellowPage对象
	 */
	public static YellowPage parseFromHTML(String htmlStr , YellowPage yellowPage){
		// 标题
		List<String> titles = ParserUtils.findWithPrefixAndSuffix("<h1 itemprop=\"name\">" , "</h1>" , htmlStr);
		String title = "";
		if(titles.size() > 0){
			title = titles.get(0);
			title = Jsoup.parse(title).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			title = StringEscapeUtils.unescapeHtml4(title); // 去转义字符，  &gt;  转换成>符号
			title = ParserUtils.trim(title);
		}
		yellowPage.setTitle(title);
				
		// 地址，地址由两部分组成street address 和 city-state
		// 617 S International Blvd, Weslaco, TX 78596
		List<String> streets = ParserUtils.findWithPrefixAndSuffix("<p class=\"street-address\">" , "</p>" , htmlStr);
		String street = "";
		if(streets.size() > 0){
			street = streets.get(0);
			street = Jsoup.parse(street).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			street = StringEscapeUtils.unescapeHtml4(street); // 去转义字符，  &gt;  转换成>符号
			street = ParserUtils.trim(street);
		}
		List<String> states = ParserUtils.findWithPrefixAndSuffix("<p class=\"city-state\">" , "</p>" , htmlStr);
		String state = "";
		if(states.size() > 0){
			state = states.get(0);
			state = Jsoup.parse(state).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			state = StringEscapeUtils.unescapeHtml4(state); // 去转义字符，  &gt;  转换成>符号
			state = ParserUtils.trim(state);
		}	
		yellowPage.setAddress(street+state);
		
		//  电话
		List<String> telephones = ParserUtils.findWithPrefixAndSuffix("<p class=\"phone\">" , "</p>" , htmlStr);
		String telephone = "";
		if(telephones.size() > 0){
			telephone = telephones.get(0);
			telephone = Jsoup.parse(telephone).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			telephone = StringEscapeUtils.unescapeHtml4(telephone); // 去转义字符，  &gt;  转换成>符号
			telephone = ParserUtils.trim(telephone);
		}	
		yellowPage.setTelephone(telephone);
		
		//  website
		List<String> websites = ParserUtils.findWithPrefixAndSuffix("\"dku\":\"" , "\",\"supermedia\":true" , htmlStr);
		String website = "";
		if(websites.size() > 0){
			website = websites.get(0);
			website = Jsoup.parse(website).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			website = StringEscapeUtils.unescapeHtml4(website); // 去转义字符，  &gt;  转换成>符号
			website = ParserUtils.trim(website);
		}	
		yellowPage.setWebsite(website);
		
		//  email
		List<String> emails = ParserUtils.findWithPrefixAndSuffix("<a href=\"mailto:" , "\" data" , htmlStr);
		String email = "";
		if(emails.size() > 0){
			email = emails.get(0);
			email = Jsoup.parse(email).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			email = StringEscapeUtils.unescapeHtml4(email); // 去转义字符，  &gt;  转换成>符号
			email = ParserUtils.trim(email);
		}	
		yellowPage.setEmail(email);
		
		/*
		// business details
		List<String> businessDetails = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>" , "</dd>" , htmlStr);
		StringBuffer businessDetail = new StringBuffer();
		for(String bd : businessDetails){
			bd = Jsoup.parse(bd).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			bd = StringEscapeUtils.unescapeHtml4(bd); // 去转义字符，  &gt;  转换成>符号
			bd = ParserUtils.trim(bd);
			businessDetail.append(bd).append("~");
		}	
		yellowPage.setBusinessDetails(businessDetail.toString());
		*/
		
		/*
		// 搜索BusinessDetails里有的属性
		List<String> businessDetailAttrs = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>" , "</dd>" , htmlStr);
		for(String a : businessDetailAttrs){
			a = Jsoup.parse(a).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			a = StringEscapeUtils.unescapeHtml4(a); // 去转义字符，  &gt;  转换成>符号
			a = ParserUtils.trim(a);
			String aa = a.substring(0 , a.indexOf(":"));
			UpdateYellowPage.BusinessDetailSet.add(aa);
		}
		*/
		
		// Social Links
		List<String> socialLinks = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Social Links:</dt>" , "</dd>" , htmlStr);
		String socialLink = "";
		StringBuffer sLink = new StringBuffer();
		if(socialLinks.size() > 0){
			socialLink = socialLinks.get(0);
			List<String> sLinks =  ParserUtils.findWithPrefixAndSuffix("<a href=\"" , "\"" , socialLink);
			for(String link : sLinks){
				link = Jsoup.parse(link).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				link = StringEscapeUtils.unescapeHtml4(link); // 去转义字符，  &gt;  转换成>符号
				link = ParserUtils.trim(link);
				sLink.append(link).append("|");
			}
		}	
		yellowPage.setSocialLinks(sLink.toString());
		
		// Services/Products
		List<String> servicesProducts = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Services/Products:</dt>" , "</dd>" , htmlStr);
		String servicesProduct = "";
		if(servicesProducts.size() > 0){
			servicesProduct = servicesProducts.get(0);
			servicesProduct = Jsoup.parse(servicesProduct).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			servicesProduct = StringEscapeUtils.unescapeHtml4(servicesProduct); // 去转义字符，  &gt;  转换成>符号
			servicesProduct = ParserUtils.trim(servicesProduct);
		}	
		yellowPage.setServicesProducts(servicesProduct);
		
		// Neighborhoods
		List<String> Neighborhoods = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Neighborhood[s]?:</dt>" , "</dd>" , htmlStr);
		String neighborhood = "";
		if(Neighborhoods.size() > 0){
			neighborhood = Neighborhoods.get(0);
			neighborhood = Jsoup.parse(neighborhood).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			neighborhood = StringEscapeUtils.unescapeHtml4(neighborhood); // 去转义字符，  &gt;  转换成>符号
			neighborhood = ParserUtils.trim(neighborhood);
		}	
		yellowPage.setNeighborhoods(neighborhood);
		
		// Hours
		List<String> Hours = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Hours:</dt>" , "</dd>" , htmlStr);
		String hour = "";
		if(Hours.size() > 0){
			hour = Hours.get(0);
			hour = Jsoup.parse(hour).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			hour = StringEscapeUtils.unescapeHtml4(hour); // 去转义字符，  &gt;  转换成>符号
			hour = ParserUtils.trim(hour);
		}	
		yellowPage.setHours(hour);
		
		// Payment method
		List<String> paymentMethods = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Payment method:</dt>" , "</dd>" , htmlStr);
		String paymentMethod = "";
		if(paymentMethods.size() > 0){
			paymentMethod = paymentMethods.get(0);
			paymentMethod = Jsoup.parse(paymentMethod).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			paymentMethod = StringEscapeUtils.unescapeHtml4(paymentMethod); // 去转义字符，  &gt;  转换成>符号
			paymentMethod = ParserUtils.trim(paymentMethod);
		}	
		yellowPage.setPaymentMethod(paymentMethod);
		
		// General Info
		List<String> generalInfos = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>General Info:</dt>" , "</dd>" , htmlStr);
		String generalInfo = "";
		if(generalInfos.size() > 0){
			generalInfo = generalInfos.get(0);
			generalInfo = Jsoup.parse(generalInfo).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			generalInfo = StringEscapeUtils.unescapeHtml4(generalInfo); // 去转义字符，  &gt;  转换成>符号
			generalInfo = ParserUtils.trim(generalInfo);
		}	
		yellowPage.setGeneralInfo(generalInfo);
		
		// Location
		List<String> locations = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Location:</dt>" , "</dd>" , htmlStr);
		String location = "";
		if(locations.size() > 0){
			location = locations.get(0);
			location = Jsoup.parse(location).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			location = StringEscapeUtils.unescapeHtml4(location); // 去转义字符，  &gt;  转换成>符号
			location = ParserUtils.trim(location);
		}	
		yellowPage.setLocation(location);
		
		// Price Range
		List<String> priceRanges = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Price Range:</dt>" , "</dd>" , htmlStr);
		String priceRange = "";
		if(priceRanges.size() > 0){
			priceRange = priceRanges.get(0);
			priceRange = Jsoup.parse(priceRange).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			priceRange = StringEscapeUtils.unescapeHtml4(priceRange); // 去转义字符，  &gt;  转换成>符号
			priceRange = ParserUtils.trim(priceRange);
		}	
		yellowPage.setPriceRange(priceRange);
		
		// Extra Phones
		List<String> extraPhones = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Extra Phones:</dt>" , "</dd>" , htmlStr);
		String extraPhone = "";
		if(extraPhones.size() > 0){
			extraPhone = extraPhones.get(0);
			extraPhone = Jsoup.parse(extraPhone).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			extraPhone = StringEscapeUtils.unescapeHtml4(extraPhone); // 去转义字符，  &gt;  转换成>符号
			extraPhone = ParserUtils.trim(extraPhone);
		}	
		yellowPage.setExtraPhones(extraPhone);
		
		// Brands
		List<String> brands = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Brands:</dt>" , "</dd>" , htmlStr);
		String brand = "";
		if(brands.size() > 0){
			brand = brands.get(0);
			brand = Jsoup.parse(brand).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			brand = StringEscapeUtils.unescapeHtml4(brand); // 去转义字符，  &gt;  转换成>符号
			brand = ParserUtils.trim(brand);
		}	
		yellowPage.setBrands(brand);
		
		// Categories
		List<String> categories = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Categor\\p{Lower}{1,3}+:</dt>" , "</dd>" , htmlStr);
		String category = "";
		if(categories.size() > 0){
			category = categories.get(0);
			category = Jsoup.parse(category).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			category = StringEscapeUtils.unescapeHtml4(category); // 去转义字符，  &gt;  转换成>符号
			category = ParserUtils.trim(category);
		}	
		yellowPage.setCategories(category);
		
		// Languages
		List<String> languages = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Languages:</dt>" , "</dd>" , htmlStr);
		String language = "";
		if(languages.size() > 0){
			language = languages.get(0);
			language = Jsoup.parse(language).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			language = StringEscapeUtils.unescapeHtml4(language); // 去转义字符，  &gt;  转换成>符号
			language = ParserUtils.trim(language);
		}	
		yellowPage.setLanguages(language);
		
		// Accreditation
		List<String> accreditations = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Accreditation:</dt>" , "</dd>" , htmlStr);
		String accreditation = "";
		if(accreditations.size() > 0){
			accreditation = accreditations.get(0);
			accreditation = Jsoup.parse(accreditation).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			accreditation = StringEscapeUtils.unescapeHtml4(accreditation); // 去转义字符，  &gt;  转换成>符号
			accreditation = ParserUtils.trim(accreditation);
		}	
		yellowPage.setAccreditation(accreditation);
		
		// Other Links and Other Link
		List<String> otherLinks = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Other Link[s]?:</dt>" , "</dd>" , htmlStr);
		String otherLink = "";
		if(otherLinks.size() > 0){
			otherLink = otherLinks.get(0);
			otherLink = Jsoup.parse(otherLink).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			otherLink = StringEscapeUtils.unescapeHtml4(otherLink); // 去转义字符，  &gt;  转换成>符号
			otherLink = ParserUtils.trim(otherLink);
		}	
		yellowPage.setOtherlinks(otherLink);
		
		// AKA
		List<String> akas = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>AKA:</dt>" , "</dd>" , htmlStr);
		String aka = "";
		StringBuffer akaSB = new StringBuffer();
		if(akas.size() > 0){
			aka = akas.get(0);
			List<String> akaPs =  ParserUtils.findWithPrefixAndSuffix("<p>","</p>" , aka);
			for(String ap : akaPs){
				ap = Jsoup.parse(ap).text(); // 去html标签 ,去换行\t\n\x0B\f\r
				ap = StringEscapeUtils.unescapeHtml4(ap); // 去转义字符，  &gt;  转换成>符号
				ap = ParserUtils.trim(ap);
				akaSB.append(ap).append("|");
			}
		}	
		yellowPage.setAka(akaSB.toString());
		
		// Amenities
		List<String> amenities = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Amenities:</dt>" , "</dd>" , htmlStr);
		String amenitie = "";
		if(amenities.size() > 0){
			amenitie = amenities.get(0);
			amenitie = Jsoup.parse(amenitie).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			amenitie = StringEscapeUtils.unescapeHtml4(amenitie); // 去转义字符，  &gt;  转换成>符号
			amenitie = ParserUtils.trim(amenitie);
		}	
		yellowPage.setAmenities(amenitie);
		
		// Associations
		List<String> associations = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Associations:</dt>" , "</dd>" , htmlStr);
		String association = "";
		if(associations.size() > 0){
			association = associations.get(0);
			association = Jsoup.parse(association).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			association = StringEscapeUtils.unescapeHtml4(association); // 去转义字符，  &gt;  转换成>符号
			association = ParserUtils.trim(association);
		}	
		yellowPage.setAssociations(association);
		
		// Other Email
		List<String> otherEmails = ParserUtils.findWithPrefixAndSuffix("<dt\\p{Print}*?>Other Email:</dt>" , "</dd>" , htmlStr);
		String otherEmail = "";
		StringBuffer otherEmailSB = new StringBuffer();
		if(otherEmails.size() > 0){
			otherEmail = otherEmails.get(0);
			List<String> oes = ParserUtils.findWithPrefixAndSuffix("mailto:" , "\"" , otherEmail);
			for(String oe : oes){
				otherEmailSB.append(oe).append("|");
			}
			otherEmail = Jsoup.parse(otherEmail).text(); // 去html标签 ,去换行\t\n\x0B\f\r
			otherEmail = StringEscapeUtils.unescapeHtml4(otherEmail); // 去转义字符，  &gt;  转换成>符号
			otherEmail = ParserUtils.trim(otherEmail);
		}	
		yellowPage.setOtherEmail(otherEmailSB.toString());
		
		return yellowPage;
	}
	
	
	public static void main(String[] args) throws IOException{
		FileReader reader = new FileReader ("e:\\aaa.html");
		StringBuilder sb = new StringBuilder();
	    BufferedReader br = new BufferedReader(reader);
	    String line;
	    while ( (line=br.readLine()) != null) {
	      sb.append(line);
	    }
	    String htmlStr = sb.toString();
	    
	 // 查找email
	 		Pattern p = Pattern.compile("[\\w[.-]]+@[\\w[.-]]+\\.[\\w]+");
	 		Matcher matcher = p.matcher(htmlStr);
	 		while(matcher.find()){
	 			String email = matcher.group(); 			
	 			System.out.println("find email: "+email);
	 		}
	    
	    br.close();
	}

}
