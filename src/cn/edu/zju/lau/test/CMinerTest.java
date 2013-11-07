package cn.edu.zju.lau.test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.edu.zju.lau.cminer.CMiner;
import cn.edu.zju.lau.cminer.model.Rule;

/**
 * CMiner测试类
 * @author yuki
 * @date 2013-11-06
 */
public class CMinerTest {

	private static CMiner miner = new CMiner();;
	
	public static void main(String[] args){

		String sequence = getRandomStr(409); //"abcedabcefagbchabijcaklcijcaklc";
		int windowSize = 50;
		int maxGap = 5;
		int minSupport = 4;
		float minConfidence = 1.0F;
		
		// 输出关联关系挖掘过程中的每一步中间结果
		// testByStep(sequence, windowSize, maxGap, minSupport, minConfidence);
		
		// 直接挖掘关联序列
		long start = System.currentTimeMillis();
		Map<String, Rule> rules = miner.startMining(sequence, windowSize, maxGap, minSupport, minConfidence);
		long end = System.currentTimeMillis();
		
		System.out.println("\n======== Generating Correlation Rules ========");
		System.out.println("Sequence: \t" + sequence);
		System.out.println("Max Gap: \t" + maxGap);
		System.out.println("Min Support: \t" + minSupport);
		System.out.println("Min Confidence: " + minConfidence);
		System.out.println("Run Time: \t" + (end - start)/1000.0 + "s");
		System.out.println("Rules: ");
		System.out.println(rules);
		
	}
	
	/**
	 * 分步执行挖掘过程的每一个过程，并输出中间结果
	 * @param sequence
	 * @param windowSize
	 * @param maxGap
	 * @param minSupport
	 * @param minConfidence
	 */
	public static void testByStep(String sequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		// 分段：对初始访问序列
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
		
		// 生成：关联规则
		Map<String, Rule> rules = miner.generateRules(freSubseq, closedFreSubseq, minConfidence);
		System.out.println("\n======== Generating Correlation Rules ========");
		System.out.println("Frequent Subsequences: \t" + freSubseq);
		System.out.println("Closed Frequent Subsequences: \t" + closedFreSubseq);
		System.out.println(rules);
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
