package cn.edu.zju.lau.simulate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;

import cn.edu.zju.lau.cminer.impl.CMinerHDFS;
import cn.edu.zju.lau.cminer.model.FileAccessLog;
import cn.edu.zju.lau.cminer.model.HDFSRule;
import cn.edu.zju.lau.test.CMinerHDFSTest;

/**
 * 模拟HDFS Cache，采用LRU替换算法
 * @author yuki
 * @date 2013-11-25
 */
public class HDFSCache {

	private int cacheSize;
	private int maxGap;
	private Queue<String> cache = new LinkedList<String>();		// 文件池，用来存储fre-fetch的文件
	private Map<String, HDFSRule> rules;
	private String currentSeq;
	private int currentGap = 0;
	
	public HDFSCache(Map<String, HDFSRule> rules, int cacheSize, int maxGap){
		this.rules = rules;
		this.cacheSize = cacheSize;
		this.maxGap = maxGap;
	}
	
	public String predictNext(String in){
		
		String nextFile = null;

		// 将当前读取的文件考虑到序列中
		if(StringUtils.isEmpty(currentSeq)){
			currentSeq = in;
		}
		else{
			currentSeq += ("|" + in);
		}

		// 在规则中搜索序列，若找到，则返回
		boolean starts = false;
		for(Map.Entry<String, HDFSRule> entry: rules.entrySet()){
			if(entry.getKey().startsWith(currentSeq)){
				//【只是】以当前序列开头，非相等：保证找出a->c,a|b->这种情况
				if(entry.getKey().endsWith(currentSeq)){
					nextFile = entry.getValue().getPrediction();
				}
				else{
					starts = true;
				}
			}
		}
		
		// 规则中未找到该序列，有四种可能：
		if(currentSeq.equals(in)){
			// 1. 序列仅包含一个文件，且没有匹配的：跳过，从下一个文件重新开始
			if(!starts){
				currentSeq = null;
				currentGap = 0;
			}
			// 2. 有匹配的序列前缀，但是并未完整形成：以当前序列为基础，继续生成 
			else{
				currentGap = 0;
			}
		}
		else{
			// 1. 没有匹配到规则序列，且超过maxGap：直接以当前文件作为新序列开头，重新生成
			if((!starts) && currentGap > maxGap){
				currentSeq = in;
				currentGap = 0;
			}
			// 2. 有匹配的序列前缀，但是并未完整形成：以当前序列为基础，继续生成
			else if(starts){
				currentGap = 0;
			}
			// 3. 没有匹配的序列前缀，且不是序列开头，Gap也没有超过：让其在gap范围内继续扩展一层
			else{
				currentGap++;
			}
		}
		
		return nextFile;
	}
	
	/**
	 * 向Cache中添加文件，同时调整最近使用的文件位置。若超出Cache大小，则移除最近最少使用的那个文件。
	 * @param file
	 * @return
	 */
	public String add(String file){
		
		String out = null;
		
		if(cache.size() >= cacheSize){
			out = cache.remove();
		}
		// 先移除，再添加，保证在队尾
		cache.remove(file);
		cache.add(file);
		
		return out;
	}
	
	public boolean isHit(String file){
		return cache.contains(file);
	}
	
	public static void main(String[] args){
		
		String logPath = "D://audit.log";
		CMinerHDFS miner = new CMinerHDFS();

		List<FileAccessLog> logs = CMinerHDFSTest.getLogs(logPath);
		miner.setInputSequence(logs);
		Map<String, HDFSRule> rules = miner.startMining();
		
		HDFSCache cache = new HDFSCache(rules, 20, 3);
		
		System.out.println("** rules:");
		for(Map.Entry<String, HDFSRule> entry: rules.entrySet()){
			System.out.println(entry.getKey() + "->" + entry.getValue().getPrediction());
		}
		System.out.println();
		
		for(int i = 0; i < logs.size(); i++){
			System.out.print(logs.get(i).getSrc()+ "\t");
			System.out.println(cache.predictNext(logs.get(i).getSrc()));
		}
		
		miner.clear();
	}
}
