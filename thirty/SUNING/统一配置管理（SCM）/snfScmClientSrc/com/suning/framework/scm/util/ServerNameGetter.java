/*  1:   */ package com.suning.framework.scm.util;
/*  2:   */ 
/*  3:   */ import java.lang.reflect.Method;
/*  4:   */ 
/*  5:   */ public class ServerNameGetter
/*  6:   */ {
/*  7:14 */   private static String serverName = System.getProperty("jboss.server.name");
/*  8:   */   
/*  9:   */   static
/* 10:   */   {
/* 11:16 */     if ((serverName == null) || (serverName.equals(""))) {
/* 12:   */       try
/* 13:   */       {
/* 14:19 */         Class<?> c = Class.forName("com.ibm.websphere.runtime.ServerName");
/* 15:20 */         Method m = c.getMethod("getDisplayName", new Class[0]);
/* 16:21 */         Object o = m.invoke(c, new Object[0]);
/* 17:22 */         serverName = o.toString();
/* 18:   */       }
/* 19:   */       catch (Exception e)
/* 20:   */       {
/* 21:24 */         serverName = "defaultServer";
/* 22:   */       }
/* 23:   */     }
/* 24:28 */     if ((serverName == null) || (serverName.equals(""))) {
/* 25:29 */       serverName = "defaultServer";
/* 26:   */     }
/* 27:   */   }
/* 28:   */   
/* 29:   */   public static String getServerName()
/* 30:   */   {
/* 31:34 */     return serverName;
/* 32:   */   }
/* 33:   */ }


/* Location:           F:\thirty\SUNING\统一配置管理（SCM）\snf-scm-client-2.2.0.jar
 * Qualified Name:     com.suning.framework.scm.util.ServerNameGetter
 * JD-Core Version:    0.7.0.1
 */