"""Execute real pagination and account guards using native API-shaped doubles."""
import pathlib,subprocess,tempfile,shutil,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class SocialLibraryTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_pages_callbacks_account_and_stale_response(self):
  code=(ROOT/'inject/java/com/neverocheg/elite/SocialLibrary.java').read_text(encoding='utf8')
  for name,alias in [('X.1M7B','mock.Api'),('X.01K8','mock.Continuation'),('X.0Xou','mock.Scene'),('X.14EI','mock.AccountApi')]:code=code.replace(name,alias)
  files={'com/neverocheg/elite/SocialLibrary.java':code,
   'android/os/Looper.java':'package android.os;public class Looper{public static Looper getMainLooper(){return new Looper();}}',
   'android/os/Handler.java':'package android.os;public class Handler{public Handler(Looper l){}public void post(Runnable r){r.run();}public void postDelayed(Runnable r,long t){}}',
   'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long uptimeMillis(){return 100000;}}',
   'mock/Continuation.java':'package mock;public interface Continuation{Object getContext();void resumeWith(Object value);}',
   'mock/Scene.java':'package mock;public class Scene{public static final Scene TT_RELATION_PAGE=new Scene();public int getValue(){return 7;}}',
   'kotlin/coroutines/EmptyCoroutineContext.java':'package kotlin.coroutines;public class EmptyCoroutineContext{public static final EmptyCoroutineContext INSTANCE=new EmptyCoroutineContext();}',
   'mock/AccountApi.java':'package mock;public class AccountApi{public static AccountApi LJFF(){return new AccountApi();}public String getCurUserId(){return com.neverocheg.elite.Account.uid;}public String getCurSecUserId(){return "sec-current";}}',
   'com/ss/android/ugc/aweme/relation/model/FriendsListResponse.java':"""package com.ss.android.ugc.aweme.relation.model;import java.util.*;public class FriendsListResponse{public boolean hasMore;public String nextPageToken;public List getUserList(){return Arrays.asList(nextPageToken);}public FriendsListResponse(boolean more,String token){hasMore=more;nextPageToken=token;}}""",
   'mock/Api.java':"""package mock;import com.ss.android.ugc.aweme.relation.model.FriendsListResponse;public class Api{public static final Api LIZIZ=new Api();public static int calls;public static boolean stale;public Object LIZIZ(int scene,int count,String sec,String page,Continuation cb){if(scene!=7||count!=30||!sec.equals("sec-current"))throw new AssertionError();calls++;if(stale)com.neverocheg.elite.Account.uid="2";FriendsListResponse out=new FriendsListResponse(page.isEmpty(),page.isEmpty()?"next":"done");if(page.isEmpty())return out;cb.resumeWith(out);return new Object();}}""",
   'com/neverocheg/elite/Account.java':'package com.neverocheg.elite;public class Account{public static String uid="1";static String liveId(){return uid;}}',
   'com/neverocheg/elite/PinnedFriends.java':'package com.neverocheg.elite;import java.util.*;class PinnedFriends{static List all=new ArrayList();static void reload(){all.clear();}static void observe(List x){all.addAll(x);}}',
   'com/neverocheg/elite/NativeStreaks.java':'package com.neverocheg.elite;class NativeStreaks{static void prepare(){}static void friends(java.util.List<?> users,String uid){}}',
   'com/neverocheg/elite/FavouriteStickers.java':'package com.neverocheg.elite;class FavouriteStickers{static void load(Runnable r){}}',
   'com/neverocheg/elite/Diary.java':'package com.neverocheg.elite;class Diary{static void note(String s){}}',
   'com/neverocheg/elite/Net.java':'package com.neverocheg.elite;class Net{static void away(String s,Runnable r){r.run();}}',
   'com/neverocheg/elite/NativeRead.java':(ROOT/'inject/java/com/neverocheg/elite/NativeRead.java').read_text(encoding='utf8'),
   'com/neverocheg/elite/Test.java':"""package com.neverocheg.elite;public class Test{public static void main(String[] a)throws Exception{SocialLibrary.load("1",null);if(mock.Api.calls!=2||PinnedFriends.all.size()!=2)throw new AssertionError("both immediate and suspended pages");PinnedFriends.all.clear();mock.Api.stale=true;SocialLibrary.load("1",null);if(!PinnedFriends.all.isEmpty())throw new AssertionError("stale account accepted");System.out.println("pagination and account guards passed");}}"""}
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory)
   for name,text in files.items():p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf8')
   out=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(work)]+[str(work/name) for name in files],capture_output=True,text=True);self.assertEqual(0,out.returncode,out.stderr)
   out=subprocess.run(['java','-cp',str(work),'com.neverocheg.elite.Test'],capture_output=True,text=True,timeout=30);self.assertEqual(0,out.returncode,out.stderr)
