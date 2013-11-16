package cn.edu.zju.lau.utils;

import java.util.Random;

/**
 * 工具类
 * 
 * @author yuki
 * @date 2013-11-16
 */
public class Utils {
	
	/**
	 * 产生一个长度为length的随机字符串
	 * @param length
	 * @return
	 */
	public static String getRandomStr(int length) {  
	    String baseStr = "abcdefghijklmnopqrstuvwxyz";//ABCDEFGHIJKLMNOPQRSTUVWXYZ";  
	    int range = baseStr.length();
	    Random random = new Random();  
	    StringBuilder strBuilder = new StringBuilder();
	    
	    for(int i = 0; i < length; i++) {   
	    	strBuilder.append(baseStr.charAt(random.nextInt(range)));  
	    }
	    
	    return strBuilder.toString();  
	} 
}
