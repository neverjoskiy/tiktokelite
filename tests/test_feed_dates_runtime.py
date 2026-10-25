"""FeedDates actually leaves old posts out of the feed page.

4.0 shipped FeedDates.java with no caller and no settings row: the class was
complete, but nothing in the mod ever read `FeedDates.active()` and nothing in
the settings wrote `date_filter_hours`. This test keeps the wiring honest: the
real `Feed.without` must drop a post older than the chosen age, and keep a
fresh post, one with no timestamp and the person's own post.
"""
import pathlib, shutil, subprocess, tempfile, unittest
from test_settings_safety_runtime import method

ROOT = pathlib.Path(__file__).resolve().parents[1]

HARNESS = '''package com.neverocheg.elite;
import com.ss.android.ugc.aweme.feed.model.Aweme;
import java.util.*;
public class Test {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] a){
        android.content.Context ctx=new android.content.Context();
        Margy.ctx=ctx;
        long now=System.currentTimeMillis()/1000L;
        Aweme fresh=new Aweme();fresh.id="fresh";fresh.created=now-3600L;
        Aweme old=new Aweme();old.id="old";old.created=now-10L*86400L;
        Aweme unknown=new Aweme();unknown.id="unknown";unknown.created=0;
        Aweme own=new Aweme();own.id="mine";own.created=now-10L*86400L;own.author="me";
        FeedDates.setHours(24);
        check(FeedDates.active(),"the chosen age turns the filter on");
        check(FeedDates.leavesOut(now-10L*86400L),"a ten-day-old post is out");
        check(!FeedDates.leavesOut(now-3600L),"a one-hour-old post stays");
        check(!FeedDates.leavesOut(0),"an unknown timestamp never counts as old");
        check(FeedDates.leavesOut(now*1000L-3L*86400L*1000L),"millisecond timestamps are counted too");
        List page=new ArrayList(Arrays.asList(fresh,old,own,unknown));
        com.ss.android.ugc.aweme.feed.model.FeedItemList wrapped=new com.ss.android.ugc.aweme.feed.model.FeedItemList();
        wrapped.items=page;
        List kept=Feed.getItems(wrapped);
        check(kept.size()==3&&!contains(kept,old),"the old post is gone; fresh, untimed and own stay");
        check(page.size()==4,"the page itself is not touched");
        FeedDates.setHours(0);
        check(FeedDates.hours()==0&&!FeedDates.active(),"turned off");
        check(Feed.getItems(wrapped)==wrapped.items,"turned off, the page is handed back untouched");
        System.out.println("FeedDates filter passed");
    }
    static boolean contains(java.util.List l,Object o){for(Object x:l)if(x==o)return true;return false;}
}
'''

MODEL = '''package com.ss.android.ugc.aweme.feed.model;
public class Aweme{public String id;public long created;public String author;public boolean ad;
public String getAid(){return id;}public long getCreateTime(){return created;}
public String getAuthorUid(){return author;}public boolean isAd(){return ad;}
public int getAwemeType(){return 0;}public long getLiveId(){return 0;}
public Object getRoomFeedCellStruct(){return null;}public Object getPhotoModeImageInfo(){return null;}
public String getDesc(){return null;}}
'''


class FeedDatesRuntimeTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_feed_drops_old_posts_and_keeps_mine_and_fresh(self):
        sources = {
            'android/content/Context.java':
                'package android.content;public class Context{public static int MODE_PRIVATE=0;'
                'public java.util.Map<String,Memory> stores=new java.util.HashMap<>();'
                'public android.content.SharedPreferences getSharedPreferences(String n,int m){'
                'android.content.SharedPreferences p=new Memory();return p;}'
                'public static class Memory implements android.content.SharedPreferences{'
                'java.util.Map<String,Object> values=new java.util.HashMap<>();'
                'public java.util.Map<String,?> getAll(){return values;}'
                'public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}'
                'public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}'
                'public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}'
                'public boolean contains(String k){return values.containsKey(k);}'
                'public Editor edit(){return new Editor(){java.util.Map<String,Object> writes=new java.util.HashMap<>();'
                'public Editor putString(String k,String v){writes.put(k,v);return this;}'
                'public Editor putBoolean(String k,boolean v){writes.put(k,v);return this;}'
                'public Editor putInt(String k,int v){writes.put(k,v);return this;}'
                'public Editor putLong(String k,long v){writes.put(k,v);return this;}'
                'public Editor putFloat(String k,float v){writes.put(k,v);return this;}'
                'public Editor remove(String k){writes.put(k,null);return this;}'
                'public void apply(){for(java.util.Map.Entry<String,Object> e:writes.entrySet())if(e.getValue()==null)values.remove(e.getKey());else values.put(e.getKey(),e.getValue());}};}}}',
            'android/content/SharedPreferences.java':
                'package android.content;public interface SharedPreferences{'
                'java.util.Map<String,?> getAll();String getString(String k,String d);'
                'boolean getBoolean(String k,boolean d);int getInt(String k,int d);boolean contains(String k);'
                'Editor edit();interface Editor{Editor putString(String k,String v);'
                'Editor putBoolean(String k,boolean v);Editor putInt(String k,int v);'
                'Editor putLong(String k,long v);Editor putFloat(String k,float v);'
                'Editor remove(String k);void apply();}}',
            'com/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeedList.java':
                (ROOT / 'inject/stubs/com/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeedList.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/NativeRead.java': (ROOT / 'inject/java/com/neverocheg/elite/NativeRead.java').read_text(encoding='utf-8'),
            'com/ss/android/ugc/aweme/feed/model/Aweme.java':
                'package com.ss.android.ugc.aweme.feed.model;'
                'public class Aweme{public String id;public long created;public String author;public boolean ad;'
                'public String getAid(){return id;}public long getCreateTime(){return created;}'
                'public String getAuthorUid(){return author;}public boolean isAd(){return ad;}'
                'public int getAwemeType(){return 0;}public long getLiveId(){return 0;}'
                'public Object getRoomFeedCellStruct(){return null;}public Object getPhotoModeImageInfo(){return null;}}',
            'com/ss/android/ugc/aweme/feed/model/FeedItemList.java':
                'package com.ss.android.ugc.aweme.feed.model;'
                'public class FeedItemList{public java.util.List items;public java.util.List getItems(){return items;}}',
            'com/ss/android/ugc/aweme/feed/model/PhotoModeImageInfo.java':
                'package com.ss.android.ugc.aweme.feed.model;public class PhotoModeImageInfo{}',
            'com/neverocheg/elite/Margy.java':
                'package com.neverocheg.elite;class Margy{public static final String PREFS="mod";'
                'static android.content.Context ctx;'
                'static android.content.Context context(){return ctx;}}',
            'com/neverocheg/elite/Plugins.java':
                'package com.neverocheg.elite;class Plugins{static java.util.List feed(java.util.List l){return l;}}',
            'com/neverocheg/elite/Probe.java':
                'package com.neverocheg.elite;class Probe{static final int FEED=1;static long start(){return 0;}static void stop(int t,long p){}}',
            'com/neverocheg/elite/Diary.java':
                'package com.neverocheg.elite;class Diary{static java.util.List<String> said=new java.util.ArrayList<>();static void note(String line){said.add(line);}}',
            'com/neverocheg/elite/Account.java':
                'package com.neverocheg.elite;class Account{static String id(){return "me";}}',
            'com/neverocheg/elite/Tags.java':
                'package com.neverocheg.elite;class Tags{static boolean blocks(Object p){return false;}static java.util.List<String> all(){return java.util.Collections.emptyList();}}',
            'com/neverocheg/elite/FeedBlacklist.java':
                'package com.neverocheg.elite;class FeedBlacklist{static java.util.List items(){return java.util.Collections.emptyList();}static java.util.List cards(){return java.util.Collections.emptyList();}static boolean hides(Object p){return false;}}',
            'com/neverocheg/elite/Watched.java':
                'package com.neverocheg.elite;class Watched{static boolean hides(){return false;}static boolean seen(String a){return false;}}',
            'com/neverocheg/elite/Text.java':
                'package com.neverocheg.elite;class Text{static final String DATE_FILTER_OFF="Выключено";'
                'static String dateFilterRecent(int n,int u){return "not older";}'
                'static String dateUnit(int u,int n){return "hours";}}',
            'com/neverocheg/elite/Feed.java': (ROOT / 'inject/java/com/neverocheg/elite/Feed.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/FeedDates.java': (ROOT / 'inject/java/com/neverocheg/elite/FeedDates.java').read_text(encoding='utf-8'),
            'com/neverocheg/elite/Test.java': HARNESS,
        }
        with tempfile.TemporaryDirectory() as folder:
            files=[]
            for name,source in sources.items():
                p=pathlib.Path(folder)/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source,encoding='utf-8');files.append(str(p))
            result=subprocess.run(['javac','-encoding','UTF-8','-d',folder,*files],capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stderr)
            result=subprocess.run(['java','-cp',folder,'com.neverocheg.elite.Test'],capture_output=True,text=True,timeout=30)
            self.assertEqual(0,result.returncode,result.stderr)
