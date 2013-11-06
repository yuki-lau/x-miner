package cn.edu.zju.lau.test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.edu.zju.lau.cminer.CMiner;

public class CMinerTest {

	public static void main(String[] args){
		
		CMiner miner = new CMiner();
		String sequence = "abcedabcefagbchabijcaklcijcaklc"; //getRandomStr(26);
		int windowSize = 5;
		int maxGap = 4;
		int minSupport = 4;
		
		// 测试：对初始访问序列分段
		List<String> accessSegments = miner.cutAccessSequence(sequence, windowSize);
		System.out.println("\n======== Cut Access Sequence ========");
		System.out.println("Sequence: \t" + sequence);
		System.out.println("Window size: \t" + windowSize);
		System.out.println(accessSegments);
		
		// 挖掘：频繁子序列
		Map<String, Integer> freSubseq = miner.candidateFreSubsequences(accessSegments, windowSize, maxGap, minSupport);
		System.out.println("\n======== Mining Frequent Subsequences ========");
		System.out.println("Max Gap: \t" + maxGap);
		System.out.println("Min Support: \t" + minSupport);
		System.out.println(freSubseq);
		
		// 过滤：Closed频繁子序列
		Map<String, Integer> closedFreSubseq = miner.closedFreSubsequences(freSubseq);
		System.out.println("\n======== Filtering Closed Frequent Subsequences ========");
		System.out.println("Sequence: \t" + sequence);
		System.out.println("Access Segments: \t" + accessSegments);
		System.out.println("Frequent Subsequences: \t" + freSubseq);
		System.out.println(closedFreSubseq);
	}
	
	/**
	 * 产生一个长度为length的随机字符串
	 * @param length
	 * @return
	 */
	public static String getRandomStr(int length) {  
	    String baseStr = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";  
	    int range = baseStr.length();
	    Random random = new Random();  
	    StringBuilder strBuilder = new StringBuilder();
	    
	    for(int i = 0; i < length; i++) {   
	    	strBuilder.append(baseStr.charAt(random.nextInt(range)));  
	    }
	    
	    return strBuilder.toString();  
	} 
}
