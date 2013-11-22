package cn.edu.zju.lau.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.lau.cminer.impl.CMinerHDFS;
import cn.edu.zju.lau.cminer.model.FileAccessLog;
import cn.edu.zju.lau.cminer.model.HDFSSubseqSuffix;

/**
 * CMinerHDFS测试类
 * @author yuki
 * @date 2013-11-22
 */
public class CMinerHDFSTest {
	
	private static final String LOG_PATH = "data/audit.log";
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
	private static List<FileAccessLog> getLogs(String filePath){
		
		List<FileAccessLog> logs = new ArrayList<FileAccessLog>();
		File file = new File(filePath);
        BufferedReader reader = null;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            String logLine = null;
            while ((logLine = reader.readLine()) != null) {
            	FileAccessLog log = FileAccessLog.parse(logLine);
            	if(log.isValid()){
            		logs.add(log);
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
		
		// 读取文件
		List<FileAccessLog> logs = getLogs(filePath);
		miner.setInputSequence(logs);
		System.out.println(logs);
		
		// 对文件访问日志分段
		miner.cutAccessSequence();
		System.out.println(miner.getInputSegments());
		
		// 获取长度为1的频繁序列
		miner.generateFirstDs();
		System.out.println(miner.getDs());
		
		// 挖掘：频繁子序列
		HDFSSubseqSuffix ss = miner.getSeqFromDs();
		miner.candidateFreSubsequences(ss.getSubsequence(), ss.getOccurTimes());
		System.out.println(miner.getFreSubsequences());

		// 过滤：Closed频繁子序列
		miner.closedFreSubsequences();
		System.out.println(miner.getClosedFreSubsequences());
		
		// 生成：关联规则
		miner.generateRules();
		System.out.println(miner.getRules());
		
		miner.clear();
	}
}
