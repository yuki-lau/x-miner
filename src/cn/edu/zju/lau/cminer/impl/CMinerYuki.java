package cn.edu.zju.lau.cminer.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.edu.zju.lau.cminer.CMinerBase;

/**
 * 挖掘序列中事件（字符）的关联关系
 * 实现《C-Miner: Mining Block Correlations in Storage Systems》中所述的C-Miner算法结果，
 * 但具体生成Candidate Frequent Subsequences算法不同：BFS，多轮扫描。
 * 
 * @author yuki
 * @date 2013-11-16
 */
public class CMinerYuki extends CMinerBase {

	public CMinerYuki(){
		super();
	}
	
	public CMinerYuki(String inputSequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		super(inputSequence, windowSize, maxGap, minSupport, minConfidence);
	}
	
	@Override
	public void candidateFreSubsequences() {

		// 检查文件访问序列的分段是否为空
		if(inputSegments == null || inputSegments.size() == 0){
			System.out.println("Input Sequeueces Segments is null! Exit...");
			return;
		}
		
		Map<String, Integer> newSequences = new HashMap<String, Integer>();
		int currentSeqLen = 0;
		
		// 子序列长度没有限制，因此最大可以为windowSize，因此执行windowSize轮扫描
		while(++currentSeqLen <= windowSize){

			// 挖掘关联子序列
			for(String segment: inputSegments){
				// 初始第一轮扫描，获取最基础（每个字符）的出现次数
				if(1 == currentSeqLen){
					for(int i = 0; i < segment.length(); i++){
						String oneCharStr = segment.substring(i, i + 1);
						Integer count = newSequences.get(oneCharStr) == null ? 0 : newSequences.get(oneCharStr);
						newSequences.put(oneCharStr, count + 1);
					}
				}
				
				// length > 1的序列挖掘
				else{
					for(String prefix: freSubsequences.keySet()){
						
						// 序列的每次扫描长度 = 当前最长序列 长度+ 1
						if(prefix.length() < currentSeqLen - 1){
							continue;
						}
						
						// 这个POS的判断：
						// 1. 顺序（非连续）包含该prefix中的所有字符即可
						// 2. POS的值为prefix中最后一个字符在segment中的后一个位置
						int pos = -1;
						int lastPos = -1;
						for(char c: prefix.toCharArray()){
							lastPos = pos;
							pos = segment.indexOf(c);
							if(pos < 0 || pos <= lastPos){
								break;
							}
						}
						if(pos < 0 || pos <= lastPos){
							continue;
						}
						
						// 以prefix为关联序列开始，在segment中继续延伸关联序列
						for(int i = pos + 1; i < segment.length() && (i - pos - 1) <= maxGap; i++){
							String oneCharStr = segment.substring(i, i + 1);
							if(prefix.contains(oneCharStr)){
								continue;
							}

							String newSeq = prefix + oneCharStr;
							Integer count = newSequences.get(newSeq) == null ? 0 : newSequences.get(newSeq);
							newSequences.put(newSeq, count + 1);
						}
					}
				}
			}
			
			// 去除support < minSupport的序列
			Iterator<Map.Entry<String, Integer>> it = newSequences.entrySet().iterator();  
	        while(it.hasNext()){  
	            if(it.next().getValue() < minSupport){
					it.remove();
				} 
	        }  
	        
	        
	        // 添加层次为currentSeqLen的subsequenceTier元素
	        Map<String, Integer> tierMap = new HashMap<String, Integer>();
	        freSubsequencesTier.put(currentSeqLen, tierMap);
	        
	        // 将newSequences添加到subSequences、根据subSequence长度划分的Map中
	        for(Map.Entry<String, Integer> entry: newSequences.entrySet()){
	        	
	        	freSubsequences.put(entry.getKey(), entry.getValue());
	        	
	        	// 对得到的frequent subsequence根据subsequence的长度进行分组
	        	freSubsequencesTier.get(currentSeqLen).put(entry.getKey(), entry.getValue());
	        }
	        
	        // 更新最长序列的长度记录
        	maxSeqLength = currentSeqLen;
        	
	        newSequences.clear();
		}
	}

}
