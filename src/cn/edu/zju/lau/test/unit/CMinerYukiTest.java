package cn.edu.zju.lau.test.unit;

import java.util.Map;

import cn.edu.zju.lau.cminer.impl.CMinerYuki;
import cn.edu.zju.lau.cminer.model.Rule;
import cn.edu.zju.lau.utils.Utils;

/**
 * CMinerYuki测试类
 * @author yuki
 * @date 2013-11-06
 */
public class CMinerYukiTest {

	private static CMinerYuki miner = new CMinerYuki();
	
	public static void main(String[] args){

		// 输出关联关系挖掘过程中的每一步中间结果
//		String sequence = "abcedabcefagbchabijcaklc";
		String sequence = Utils.getRandomStr(100000);
		testByStep(sequence);
		
		// 直接挖掘关联序列
//		for(int i = 1; i <= 10; i++){
//			
//			miner.setInputSequeuece(Utils.getRandomStr(i * 100));
//			
//			long start = System.currentTimeMillis();
//			miner.startMining();
//			long end = System.currentTimeMillis();
//
//			System.out.print(miner);
//			System.out.println("Run Time: \t" + (end - start)/1000.0 + "s");
//			
//			miner.clear();
//		}
	}
	
	/**
	 * 分步执行挖掘过程的每一个过程，并输出中间结果
	 */
	public static void testByStep(String sequence){
		
		miner.setInputSequeuece(sequence);
		
		long start = System.currentTimeMillis();
		miner.cutAccessSequence();			// 分段：对初始访问序列
		miner.candidateFreSubsequences();	// 挖掘：频繁子序列
		miner.closedFreSubsequences();		// 过滤：Closed频繁子序列
		miner.generateRules();				// 生成：关联规则
		long end = System.currentTimeMillis();
		
		System.out.println("\n======== Generating Correlation Rules ========");
		System.out.println("Window size:\t" + miner.getWindowSize());
		System.out.println("Max Gap:\t" + miner.getMaxGap());
		System.out.println("Min Support:\t" + miner.getMinSupport());
		System.out.println("Min Confidence:\t" + miner.getMinConfidence());
		
		System.out.println("Input Sequence Length:\t\t" + miner.getInputSequeuece().length());
		System.out.println("Input Segments Length:\t\t" + miner.getInputSegments().size());
		System.out.println("Frequent Subsequences:\t\t" + miner.getFreSubsequences());
		System.out.println("Closed Frequent Subsequences:\t" + miner.getClosedFreSubsequences());
		System.out.println("Rules Number:\t" + miner.getRules().size());
		for(Map.Entry<String, Rule> rule: miner.getRules().entrySet()){
			System.out.println("\t" + rule);
		}
		System.out.println("Run Time: \t" + (end - start)/1000.0 + "s");
		
		miner.clear();
	}
}
