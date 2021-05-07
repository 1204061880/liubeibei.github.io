/*  1:   */ package com.suning.framework.scm.util;
/*  2:   */ 
/*  3:   */ import java.security.MessageDigest;
/*  4:   */ import org.slf4j.Logger;
/*  5:   */ import org.slf4j.LoggerFactory;
/*  6:   */ 
/*  7:   */ public class MD5Utils
/*  8:   */ {
/*  9:12 */   private static Logger logger = LoggerFactory.getLogger(MD5Utils.class);
/* 10:13 */   private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
/* 11:   */   
/* 12:   */   public static String asHex(byte[] buf)
/* 13:   */   {
/* 14:16 */     char[] chars = new char[2 * buf.length];
/* 15:17 */     for (int i = 0; i < buf.length; i++)
/* 16:   */     {
/* 17:18 */       chars[(2 * i)] = HEX_CHARS[((buf[i] & 0xF0) >>> 4)];
/* 18:19 */       chars[(2 * i + 1)] = HEX_CHARS[(buf[i] & 0xF)];
/* 19:   */     }
/* 20:21 */     return new String(chars);
/* 21:   */   }
/* 22:   */   
/* 23:   */   public static String getMD5(String s)
/* 24:   */   {
/* 25:   */     try
/* 26:   */     {
/* 27:27 */       MessageDigest digest = MessageDigest.getInstance("MD5");
/* 28:28 */       digest.update(s.getBytes("UTF-8"));
/* 29:29 */       byte[] messageDigest = digest.digest();
/* 30:30 */       return asHex(messageDigest);
/* 31:   */     }
/* 32:   */     catch (Exception e)
/* 33:   */     {
/* 34:32 */       logger.error("Exception", e);
/* 35:   */     }
/* 36:34 */     return "";
/* 37:   */   }
/* 38:   */ }


/* Location:           F:\thirty\SUNING\统一配置管理（SCM）\snf-scm-client-2.2.0.jar
 * Qualified Name:     com.suning.framework.scm.util.MD5Utils
 * JD-Core Version:    0.7.0.1
 */