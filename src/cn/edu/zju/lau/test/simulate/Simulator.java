package cn.edu.zju.lau.test.simulate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import cn.edu.zju.lau.cminer.model.hdfs.FileAccessLog;
import cn.edu.zju.lau.utils.LRUCache;

/**
 * 模拟带PredictCache使用过程的抽象类，包含：
 * 		LRUCache：模拟文件缓存
 * 		抽象方法 getPredictFiles：获取预测的后续文件，根据子类选用的挖掘关联规则的算法而实现。
 * 
 * @author yuki
 * @date 2013-11-27
 */
public abstract class Simulator {

	protected LRUCache<String, String> FILE_CACHE;
	
	protected Simulator(int fileCacheSize){
		
		this.FILE_CACHE = new LRUCache<String, String>(fileCacheSize);
	}
	
	/**
	 * 从指定的文件中读取文件访问日志数据集
	 * @param filePath
	 * @param cutCommonPrefix	需要截断的文件名公共前缀
	 * @return	文件访问序列的文件名列表
	 */
 	protected List<String> getDataSet(String filePath, String cutCommonPrefix){
		
		List<String> logs = new ArrayList<String>();

		File file = new File(filePath);
        BufferedReader reader = null;
        
        try {
            reader = new BufferedReader(new FileReader(file));
            String logLine = null;
            while ((logLine = reader.readLine()) != null) {
            	FileAccessLog log = FileAccessLog.parse(logLine);
            	if(log.isValid()){
            		if(!StringUtils.isEmpty(cutCommonPrefix)){
            			logs.add(log.getSrc().split(cutCommonPrefix)[1]);
            		}
            		else{
            			logs.add(log.getSrc());
            		}
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
	 * 从File Cache中读取文件，同时将文件加入Cache。
	 * @param fileName
	 * @return 命中返回文件名，未命中返回NULL。
	 */
	protected String getFileFromCache(String fileName){
		
		String targetFile = FILE_CACHE.get(fileName);
		if(targetFile == null){
			FILE_CACHE.put(fileName, fileName);
		}
		return targetFile;
	}
	
	/**
	 * 将fileName代表的文件放入Cache
	 * @param fileName
	 * @param file
	 */
	protected void putFileIntoCache(String fileName, String file){
		FILE_CACHE.put(fileName, file);
	}
	
	/**
	 * 抽象方法。获取预测的后续文件
	 * @param currentFile
	 * @return	后续文件列表
	 */
	protected abstract List<String> getPredictFiles(String currentFile);
 
}
