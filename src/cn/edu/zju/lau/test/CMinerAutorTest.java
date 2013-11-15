package cn.edu.zju.lau.test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.edu.zju.lau.cminer.CMinerAuthor;
import cn.edu.zju.lau.cminer.model.SubsequenceSuffix;

/**
 * CMinerAutor测试类
 * @author yuki
 * @date 2013-11-15
 */
public class CMinerAutorTest {

	private static CMinerAuthor miner = new CMinerAuthor();;
	
	public static void main(String[] args){

//		String sequence = getRandomStr(409); 
		String sequence = "abcedabcefagbchabijcaklc";
		int windowSize = 5;
		int maxGap = 3;
		int minSupport = 4;
		float minConfidence = 1.0F;
		
		// 分段：对初始访问序列
		List<String> accessSegments = miner.cutAccessSequence(sequence, windowSize);
		System.out.println(accessSegments);
		
		// 初始化：获取长度为1的频繁序列
		miner.generateFirstDs(accessSegments, minSupport);
		
		// DFS：生成候选频繁序列
		SubsequenceSuffix ss = miner.getSeqFromDs();
		miner.candidateFreSubsequences(ss.getSubsequence(), ss.getOccurTimes(), maxGap, minSupport);
	}
	

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
