/* ==================================================================   
 * Created [2006-6-19] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
*/
package com.boubei.tss.util;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * 字符串工具类：
 * 1.字符串转码工具：将字符串的编码进行转换（默认为GBK转UTF-8）
 */
public class StringUtil {
	
	/**
	 * 替换各种分隔符（tab、空格、回车、英文逗号、中文逗号）为统一分隔符
	 */
	public static String fixSplit(String s, String split) {
		return s.replaceAll("\t+|\r|\n|,|，|\\s+|、", split);
	}
	
	public static String[] split(String s) {
		return fixSplit(s, ",").split(",");
	}

    /**
     * <p>
     * 转换字符串编码方式
     * </p>
     * 
     * @param str
     * @param oldCharset
     * @param charset
     * @return
     */
    public static String convertCoding(String str, String oldCharset, String charset) {
        byte[] bytes;
        try {
            bytes = str.getBytes(oldCharset);
        } catch (UnsupportedEncodingException e1) {
            throw new RuntimeException("字符串编码转换失败，不支持的编码方式：" + oldCharset, e1);
        }
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("字符串编码转换失败，不支持的编码方式：" + charset, e);
        }
    }

    /**
     * GBK格式字符串转UTF-8格式
     */
    public static String GBKToUTF8(String str) {
        return convertCoding(str, "GBK", "UTF-8");
    }

    /**
     * UTF-8格式字符串转GBK格式
     */
    public static String UTF8ToGBK(String str) {
        return convertCoding(str, "UTF-8", "GBK");
    }
    
	static Pattern cnPattern = Pattern.compile("[\u4e00-\u9fa5]");
	public static boolean hasCNChar(String str) {
		return cnPattern.matcher(str).find();
	}
	
	public static String replaceEmoji(String s) {
		if( s == null ) return null;
		return s.replaceAll("[\\ud83c\\udc00-\\ud83c\\udfff]|[\\ud83d\\udc00-\\ud83d\\udfff]|[\\u2600-\\u27ff]", "*");
	}

	/**
	 * 判断是否为乱码
	 */
	public static boolean isMessyCode(String str) {
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			/*
			 *  当从Unicode编码向某个字符集转换时，如果在该字符集中没有对应的编码，则得到0x3f（即问号字符?）
			 *  从其他字符集向Unicode编码转换时，如果这个二进制数在该字符集中没有标识任何的字符，则得到的结果是0xfffd
			 */
			if ((int) c == 0xfffd) {
				return true;
			}
		}
		return false;
	}

    /**
     * 转换utf8字符集
     * @param str
     * @return
     */
    public static String toUtf8String(String str) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 0 && c <= 255) {
                sb.append(c);
            } else {
                byte[] b = new byte[0];
                try {
                    b = Character.toString(c).getBytes("utf-8");
                } catch (Exception ex) {
                }
                
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    k = (Integer) EasyUtils.checkTrue(k < 0, k + 256, k);
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }
}
