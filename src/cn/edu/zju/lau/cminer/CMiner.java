package cn.edu.zju.lau.cminer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CMiner {
	
	// 存储不同长度的frequent subsequence，例如 ：
	//     frequent subsequence: {abc=4, b=4, c=5, a=5, ac=5, ab=4, bc=4}
	//	   Subsequence Tier：{1={b=4, c=5, a=5}, 2={ac=5, ab=4, bc=4}, 3={abc=4}}
	private Map<Integer, Map<String, Integer>> subsequenceTier = new HashMap<Integer, Map<String, Integer>>();
	private int maxSeqLength = 0;
	
	/**
	 * 预处理：采用non-overlapped cutting方法将访问序列划分为多个固定长度的短序列片段
	 * 
	 * @param sequence		文件访问序列
	 * @param windowSize 	窗口大小，每个片段的长度
	 * @return 划分完成的短序列片段列表
	 */
	public List<String> cutAccessSequence(String sequence, int windowSize){
		List<String> accessSegments = new ArrayList<String>();
		int start = 0;
		int end = start + windowSize;
		
		while(end < sequence.length()){
			accessSegments.add(sequence.substring(start, end));
			start = end;
			end += windowSize;
		}
		accessSegments.add(sequence.substring(start));
		
		return accessSegments;
	}
	
	/**
	 * 产生候选子序列集合，满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 
	 * @param accessSegments	完成切分的访问序列片段列表
	 * @param windowSize 	窗口大小，每个片段的长度
	 * @param maxGap		序列中最大的访问间距
	 * @param minSupport	序列最小发生次数
	 * @return	candidate frequent subsequences
	 */
	public Map<String, Integer> candidateFreSubsequences(List<String> accessSegments, int windowSize, int maxGap, int minSupport){
		
		Map<String, Integer> subSequences = new HashMap<String, Integer>();
		Map<String, Integer> newSequences = new HashMap<String, Integer>();
		int currentSeqLen = 0;
		
		// 子序列长度没有限制，因此最大可以为windowSize，因此执行windowSize轮扫描
		while(++currentSeqLen <= windowSize){

			// 挖掘关联子序列
			for(String segment: accessSegments){
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
					for(String prefix: subSequences.keySet()){
						
						// 序列的每次扫描长度 = 当前最长序列 长度+ 1
						if(prefix.length() < currentSeqLen - 1){
							continue;
						}
						
						// 这个POS的判断：
						// 1. 包含该prefix中的所有字符即可
						// 2. POS的值为prefix中最后一个字符在segment中的后一个位置
						int pos = -1;
						for(char c: prefix.toCharArray()){
							pos = segment.indexOf(c);
							if(pos < 0){
								break;
							}
						}
						if(pos < 0){
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
	        
	        // 将newSequences添加到subSequences中
	        for(Map.Entry<String, Integer> entry: newSequences.entrySet()){
	        	
	        	subSequences.put(entry.getKey(), entry.getValue());
	        	
	        	// 对得到的frequent subsequence根据subsequence的长度进行分组
	        	int keyLength = entry.getKey().length();
	        	if(subsequenceTier.containsKey(keyLength)){
	        		subsequenceTier.get(keyLength).put(entry.getKey(), entry.getValue());
	        	}
	        	else{
	        		Map<String, Integer> tierMap = new HashMap<String, Integer>();
	        		tierMap.put(entry.getKey(), entry.getValue());
	        		subsequenceTier.put(keyLength, tierMap);
	        	}
	        	
	        	// 更新最长序列的长度记录
	        	if(keyLength > maxSeqLength){
	        		maxSeqLength = keyLength;
	        	}
	        	
	        }
	        
	        newSequences.clear();
		}

		return subSequences;
	}
	
	/**
	 * 从候选子序列中挑选出满足Closed性质的子序列，完成最终关联子序列挖掘
	 */
	public Map<String, Integer> closedFreSubsequences(Map<String, Integer> subSequences){
		
		Map<String, Integer> closedSubSequences = new HashMap<String, Integer>();
		
		// 每层、依次检查每一个frequent subsequence，从中挑选出closed frequent subsequence
		for(int i = this.maxSeqLength; i > 0; i--){
			
			// 最长序列都是closed的
			if(i == this.maxSeqLength){
				for(Map.Entry<String, Integer> entry: this.subsequenceTier.get(i).entrySet()){
					closedSubSequences.put(entry.getKey(), entry.getValue());
				}
			}
			
			// closed条件：
			// 不是任何frequent subsequence的子序列
			// 或
			// 是子序列 && confidence 与 大于父序列的confidence
			else{
				// 依次检查当前层每一个序列
				for(Map.Entry<String, Integer> entry: this.subsequenceTier.get(i).entrySet()){
					boolean isSubSubseq = false;
					
					// 当前序列 与 其父层（直接父序列）中每一个序列的关系：是否为子序列、confidence大小关系
					for(Map.Entry<String, Integer> superEntry: this.subsequenceTier.get(i + 1).entrySet()){
						
						// 是子序列
						if(superEntry.getKey().contains(entry.getKey())){
							
							isSubSubseq = true;
							
							// 大于所有父序列的confidence
							if(entry.getValue() > superEntry.getValue()){
								closedSubSequences.put(entry.getKey(), entry.getValue());
								
								// 虽然这一次大于，但是有可能是有小于等于的父序列的，所以需要检测完
								continue;
							}
							else{
								closedSubSequences.remove(entry.getKey());
								
								// 一票否定，无需继续检测
								break;
							}
						}
					}
					
					// 不是任何frequent subsequence的子序列
					if(!isSubSubseq){
						closedSubSequences.put(entry.getKey(), entry.getValue());
					}
					
					System.out.println(closedSubSequences);
				}
			}
		}
		
		return closedSubSequences;
	}
	
	/**
	 * 从最终关联子序列中生成correlation rules，同时进行rules的最小化工作。
	 */
	public void generateRules(){
		
	}
}
