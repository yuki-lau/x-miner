package cn.edu.zju.lau.test.unit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.edu.zju.lau.cminer.impl.hdfs.CMinerHDFS;
import cn.edu.zju.lau.cminer.model.hdfs.FileAccessLog;
import cn.edu.zju.lau.cminer.model.hdfs.HDFSRule;
import cn.edu.zju.lau.cminer.model.hdfs.HDFSSubseqSuffix;

/**
 * CMinerHDFS测试类
 * @author yuki
 * @date 2013-11-22
 */
public class CMinerHDFSTest {
	
	private static final String LOG_PATH = "D://audit.log";
	private static CMinerHDFS miner = new CMinerHDFS();
	
	public static void main(String[] args){
		
		// 使用默认参数执行挖掘，输出每一步的结果
		testByStep(LOG_PATH);
		
		// 使用自带的Mining方法，不输出每一步结果
//		List<FileAccessLog> logs = getLogs(LOG_PATH);
//		for(int i = 0; i < 10; i ++){
//			miner.setInputSequence(logs);
//			
//			long start = System.currentTimeMillis();
//			miner.startMining();
//			long end = System.currentTimeMillis();
//			
//			System.out.println((end - start) / 1000.0);
//			
//			miner.clear();
//		}
		
	}
	
	/**
	 * 从filePath处读取日志文件，并解析成FileAccessLog对象列表，返回。
	 * @param filePath
	 * @return
	 */
	public static List<String> getLogs(String filePath){
		
		List<String> logs = new ArrayList<String>();

		File file = new File(filePath);
        BufferedReader reader = null;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            String logLine = null;
            while ((logLine = reader.readLine()) != null) {
            	FileAccessLog log = FileAccessLog.parse(logLine);
            	if(log.isValid()){
            		logs.add(log.getSrc().split("/user/root/input/sogou/query-log-")[1]);
            	}
            }
        } 
        catch (IOException ioe) {
        	ioe.printStackTrace();
        } 
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } 
                catch (IOException e) {
                	e.printStackTrace();
                }
            }
        }
        
		return logs;
	}

	/**
	 * 分步执行挖掘过程的每一个过程，并输出中间结果
	 */
	public static void testByStep(String filePath){
		
		miner.setMinSupport(3);
		miner.setWindowSize(26);
		
		// 读取文件
		List<String> logs = getLogs(filePath);
		miner.setInputSequence(logs);
		System.out.println("** input sequence:");
		System.out.println(logs);
		System.out.println();
		
		// 对文件访问日志分段
		miner.cutAccessSequence();
		System.out.println("** input segments:");
		List<List<String>> segments = miner.getInputSegments();
		for(int i = 0; i < segments.size(); i++){
			List<String> segment = segments.get(i);
			for(int j = 0; j < segment.size(); j++){
				System.out.print(segment.get(j) + " ");
			}
			System.out.print(", ");
		}
		System.out.println();
		
		// 获取长度为1的频繁序列
		miner.generateFirstDs();
		// System.out.println(miner.getDs());
		
		// 挖掘：频繁子序列
		HDFSSubseqSuffix ss = miner.getSeqFromDs();
		miner.candidateFreSubsequences(ss.getSubsequence(), ss.getOccurTimes());
		System.out.println("** frequent subsequences:");
		System.out.println(miner.getFreSubsequences());

		// 过滤：Closed频繁子序列
		miner.closedFreSubsequences();
		System.out.println("** closed frequent subsequences:");
		System.out.println(miner.getClosedFreSubsequences());
		
		// 生成：关联规则
		miner.generateRules();
		System.out.println("** rules:");
		for(Map.Entry<String, HDFSRule> entry: miner.getRules().entrySet()){
			System.out.println(entry.getKey());
		}
		System.out.println();
		
		miner.clear();
	}
}
