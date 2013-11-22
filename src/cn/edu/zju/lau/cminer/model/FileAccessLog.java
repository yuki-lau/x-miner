package cn.edu.zju.lau.cminer.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 将HDFS中的audit.log中的日志行转换成日志对象。
 * Audit.log是HDFS中的监控日志，需要配置开启，它会记录所有对文件的操作。
 * 详情参见：http://wiki.apache.org/hadoop/HowToConfigure 中的 Log4j Configuration部分
 * 
 * @author yuki
 * @date 2013-11-22
 */
public class FileAccessLog {
	
	// Log记录格式：time: ugi= ip= cmd= src=	dst= perm=
	// 范例：2013-11-21 17:24:54,697: ugi=root	ip=/127.0.0.1	cmd=open	src=/input/access_log_1	dst=null	perm=null
	
	private String time;	// 客户端访问文件的时间：yyyy-MM-dd HH:mm:ss,SSS
	private String ugi;		// 客户端的用户标识
	private String ip;		// 客户端的IP
	private String cmd;		// 文件访问的具体操作，包括：open|create|delete|rename|mkdirs|listStatus|setReplication|setOwner|setPermission
	private String src;		// 文件操作中源文件地址
	private String dst;		// 文件操作中目标文件地址
	private String perm;	// 操作权限
	
	private boolean valid = true;
	private Date timeDate;	// 客户端访问文件的时间的Date对象
	private static final String timeFormat = "yyyy-MM-dd HH:mm:ss,SSS";
	
	public static FileAccessLog parse(String logLine){
		
		FileAccessLog log = new FileAccessLog();
		
		try{
			// logPargs[0]: 2013-11-21 17:24:54,697
			// logParts[1]: ugi=root	ip=/127.0.0.1	cmd=open	src=/input/access_log_1	dst=null	perm=null
			// 解析出时间和日志段
			int timeEnd = logLine.indexOf(": ");
			log.setTime(logLine.substring(0, timeEnd));
			
			String infoParts[] = logLine.substring(timeEnd + 2).split("\t");
			log.setUgi(infoParts[0].split("=")[1]);
			log.setIp(infoParts[1].split("=")[1]);
			log.setCmd(infoParts[2].split("=")[1]);
			log.setSrc(infoParts[3].split("=")[1]);
			log.setDst(infoParts[4].split("=")[1]);
			log.setPerm(infoParts[5].split("=")[1]);
			
			// 只提取只读的OPEN操作
			if(!log.getCmd().equalsIgnoreCase("open")){
				log.setValid(false);
			}
		}
		catch(Exception e){
			System.err.println("Invalid log line: " + logLine);
			log.setValid(false);
		}
		
		return log;
	}
	
	public static void main(String[] args){
		String logLine = "2013-11-21 17:25:00,448: ugi=root	ip=/127.0.0.1	cmd=create	src=/user/root/output/nasa/pv/_SUCCESS	dst=null	perm=root:supergroup:rw-r--r--";
		System.out.println(logLine + "\n");
		
		FileAccessLog logObj = FileAccessLog.parse(logLine);
		System.out.println(logObj + "\n");
        
        SimpleDateFormat df = new SimpleDateFormat(timeFormat);
        System.out.println(df.format(logObj.getTimeDate()));
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
	    sb.append("\nValid:\t\t" + this.valid);
	    sb.append("\nAccess Time:\t" + this.time);
	    sb.append("\nAccess User:\t" + this.ugi);
	    sb.append("\nRemote Addr:\t" + this.ip);
	    sb.append("\nCommand:\t" + this.cmd);
	    sb.append("\nSource Path:\t" + this.src);
	    sb.append("\nDestination:\t" + this.dst);
	    sb.append("\nPermission:\t" + this.perm);
	    sb.append("\n=======================\n");
	    return sb.toString();
	}
	
	public Date getTimeDate(){
		if(timeDate == null){
			if(time == null){
				return null;
			}
			
			try {
				SimpleDateFormat df = new SimpleDateFormat(timeFormat);
				timeDate = df.parse(time);
			} 
			catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return timeDate;
	}
	
	public String getTime() {
		return time;
	}
	
	public void setTime(String time) {
		this.time = time;
	}
	
	public String getUgi() {
		return ugi;
	}
	
	public void setUgi(String ugi) {
		this.ugi = ugi;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getCmd() {
		return cmd;
	}
	
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	
	public String getSrc() {
		return src;
	}
	
	public void setSrc(String src) {
		this.src = src;
	}
	
	public String getDst() {
		return dst;
	}
	
	public void setDst(String dst) {
		this.dst = dst;
	}
	
	public String getPerm() {
		return perm;
	}
	
	public void setPerm(String perm) {
		this.perm = perm;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
