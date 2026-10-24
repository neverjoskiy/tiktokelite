package com.neverocheg.elite;
import android.app.Dialog;import android.view.View;
final class SheetPost {
 private SheetPost(){}
 /**
  * The post a share sheet was opened for, read off the sheet itself.
  *
  * The usual way is the feed cell TikTok last selected, and sometimes there is none to hand: a
  * video opened from a profile or a search, or a cell that was recycled while the sheet came up.
  * Then nothing took the sheet over and TikTok's own download ran. The sheet keeps the post it is
  * about among its fields, so it is looked for there, one step deep. Only on the feed and the
  * video page: a photo in a comment has a screen and a "save" of its own, and the video behind it
  * is not what that one is about.
  */
 static Object of(Dialog dialog){
  try{
   android.app.Activity owner=dialog.getOwnerActivity();
   android.content.Context context=dialog.getContext();
   for(int i=0;owner==null&&i<12&&context instanceof android.content.ContextWrapper;i++){
    if(context instanceof android.app.Activity)owner=(android.app.Activity)context;
    else context=((android.content.ContextWrapper)context).getBaseContext();
   }
   String screen=owner==null?"":owner.getClass().getName();
   if(!screen.equals("com.ss.android.ugc.aweme.main.MainActivity")&&!screen.equals("com.ss.android.ugc.aweme.detail.ui.DetailActivity"))return null;
   return aweme(dialog,0,new java.util.IdentityHashMap<Object,Boolean>());
  }catch(Throwable error){Diary.note("download sheet post: "+error);return null;}
 }
 private static final String AWEME="com.ss.android.ugc.aweme.feed.model.Aweme";
 private static Object aweme(Object from,int depth,java.util.IdentityHashMap<Object,Boolean> seen){
  if(from==null||depth>2||seen.put(from,Boolean.TRUE)!=null)return null;
  for(Class<?> type=from.getClass();type!=null&&type!=Object.class&&type!=Dialog.class;type=type.getSuperclass()){
   for(java.lang.reflect.Field field:type.getDeclaredFields()){
    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||field.getType().isPrimitive())continue;
    Class<?> kind=field.getType();
    if(View.class.isAssignableFrom(kind)||android.content.Context.class.isAssignableFrom(kind)||kind==String.class)continue;
    try{
     field.setAccessible(true);Object value=field.get(from);if(value==null)continue;
     if(value.getClass().getName().equals(AWEME))return value;
     String name=value.getClass().getName();
     if(depth<2&&(name.startsWith("com.ss.")||name.startsWith("X."))){Object found=aweme(value,depth+1,seen);if(found!=null)return found;}
    }catch(Throwable ignored){}
   }
  }
  return null;
 }
}
