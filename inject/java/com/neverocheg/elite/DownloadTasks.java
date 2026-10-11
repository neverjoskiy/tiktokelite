package com.neverocheg.elite;
import java.util.*;
/** Bounded task history. Retry closures use application context, never a window. */
public final class DownloadTasks {
    public static final class Control {
        volatile long limit;
        volatile boolean cancelled;volatile java.net.HttpURLConnection connection;
        public boolean cancelled(){return cancelled;}
        void cancel(){cancelled=true;final java.net.HttpURLConnection live=connection;if(live!=null)Net.away("cancel download",()->live.disconnect());}
    }
    public static final class Task {
        public final String key,label,origin,owner;public final int percent;public final String state;public final boolean cancel,retry;
        Task(Entry e){key=e.key;label=e.label;origin=e.origin;owner=e.owner;percent=e.percent;state=e.state;cancel=!e.readOnly&&e.state.equals("Загрузка")&&!e.control.cancelled;retry=!cancel&&!e.state.equals("Отмена…")&&e.retry!=null&&!e.state.equals("Готово");}
    }
    private static final class Entry {
        String key,label,origin,owner,state="Загрузка";int percent;boolean readOnly;Control control=new Control();Runnable retry;
    }
    private static final LinkedHashMap<String,Entry> tasks=new LinkedHashMap<>();
    public static synchronized Control begin(String key,String label,String origin,Runnable retry){
        Entry old=tasks.get(key);if(old!=null&&(old.state.equals("Загрузка")||old.state.equals("Отмена…")))throw new IllegalStateException("task running");
        Entry e=new Entry();e.key=key;e.label=label;e.origin=origin;e.retry=retry;
        String handle=Account.profileHandle();e.owner=Account.liveId()==null?"Без аккаунта":handle==null?"Аккаунт "+Account.liveId():"@"+handle;
        tasks.remove(key);tasks.put(key,e);trim();return e.control;
    }
    static synchronized void observe(String key,String label,int percent){Entry e=tasks.get(key);if(e==null||e.readOnly&&!e.state.equals("Загрузка")){e=new Entry();e.key=key;e.label=label;e.origin="Мод";String handle=Account.profileHandle();e.owner=handle==null?"Этот аккаунт":"@"+handle;e.readOnly=true;tasks.put(key,e);trim();}e.percent=percent;}
    static synchronized void endObserved(String key,boolean success){Entry e=tasks.get(key);if(e!=null&&e.readOnly){e.state=success?"Готово":"Завершено";trim();}}
    private static void trim(){Iterator<Entry> i=tasks.values().iterator();while(tasks.size()>16&&i.hasNext()){Entry e=i.next();if(!e.state.equals("Загрузка")&&!e.state.equals("Отмена…"))i.remove();}}
    public static synchronized void progress(String key,int percent){Entry e=tasks.get(key);if(e!=null)e.percent=percent<0?-1:Math.min(100,percent);}
    public static synchronized void finish(String key,boolean success){Entry e=tasks.get(key);if(e==null)return;e.state=e.control.cancelled?"Отменено":success?"Готово":"Ошибка";e.control.connection=null;if(success)e.retry=null;trim();}
    public static void cancel(String key){Control control=null;synchronized(DownloadTasks.class){Entry e=tasks.get(key);if(e!=null&&e.state.equals("Загрузка")){e.state="Отмена…";control=e.control;}}if(control!=null)control.cancel();}
    public static void retry(String key){Runnable action=null;synchronized(DownloadTasks.class){Entry e=tasks.get(key);if(e!=null&&!e.state.equals("Загрузка")&&!e.state.equals("Отмена…")){action=e.retry;e.retry=null;}}if(action!=null)action.run();}
    static synchronized List<Task> snapshot(){List<Task> result=new ArrayList<>();for(Entry e:tasks.values())result.add(new Task(e));return result;}
}
