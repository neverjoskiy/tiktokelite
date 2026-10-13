"""Short counts are shortened the English way, and a shortened count is left alone."""
import pathlib,shutil,subprocess,tempfile,unittest
from test_account_appearance_runtime import STUBS
ROOT=pathlib.Path(__file__).resolve().parents[1]
class CountsTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_format_and_tag_guard(self):
  sources={k:STUBS[k] for k in ['android/content/Context.java','android/content/SharedPreferences.java']}
  sources.update({
   'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long uptimeMillis(){return 100000;}}',
   'android/view/View.java':'package android.view;public class View{public static int VISIBLE=0;int visibility;java.util.Map tags=new java.util.HashMap();public int getVisibility(){return visibility;}public Object getTag(int k){return tags.get(k);}public void setTag(int k,Object v){if(v==null)tags.remove(k);else tags.put(k,v);}public boolean postDelayed(Runnable r,long t){return true;}public android.view.ViewTreeObserver getViewTreeObserver(){return new ViewTreeObserver();}}',
   'android/view/ViewGroup.java':'package android.view;import java.util.*;public class ViewGroup extends View{List<View> kids=new ArrayList<View>();public int getChildCount(){return kids.size();}public View getChildAt(int i){return kids.get(i);}}',
   'android/view/ViewTreeObserver.java':'package android.view;public class ViewTreeObserver{public interface OnGlobalLayoutListener{void onGlobalLayout();}public boolean isAlive(){return true;}public void addOnGlobalLayoutListener(OnGlobalLayoutListener l){}public void removeOnGlobalLayoutListener(OnGlobalLayoutListener l){}}',
   'android/view/Window.java':'package android.view;public class Window{public View getDecorView(){return new View();}}',
   'android/app/Activity.java':'package android.app;public class Activity{public android.view.Window getWindow(){return new android.view.Window();}}',
   'android/widget/TextView.java':'package android.widget;public class TextView extends android.view.View{public String text="";public CharSequence getText(){return text;}public void setText(CharSequence t){text=t.toString();}}',
   'android/widget/EditText.java':'package android.widget;public class EditText extends TextView{}',
   'android/widget/Button.java':'package android.widget;public class Button extends TextView{}',
   'com/neverocheg/elite/Margy.java':'package com.neverocheg.elite;class Margy{static final String PREFS="p";static android.content.Context context(){return null;}}',
   'com/neverocheg/elite/Diary.java':'package com.neverocheg.elite;class Diary{static void note(String s){}static void detail(String s){}}',
   'com/neverocheg/elite/Watch.java':'package com.neverocheg.elite;class Watch{static android.app.Activity here(){return null;}}',
   'com/neverocheg/elite/Counts.java':(ROOT/'inject/java/com/neverocheg/elite/Counts.java').read_text(encoding='utf-8'),
   'com/neverocheg/elite/Test.java':'''package com.neverocheg.elite;import android.widget.TextView;public class Test{
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] a){
check("100,8K".equals(Counts.format("100,8 тыс.")),"thousands");
check("100,8K".equals(Counts.format("100,8 тыс")),"no dot");
check("100,8K".equals(Counts.format("100,8\\u202fтыс.")),"narrow no-break space");
check("1,2M".equals(Counts.format("1,2 млн.")),"millions");
check("3B".equals(Counts.format("3 млрд")),"billions");
check("2,5K".equals(Counts.format("2,5 тис.")),"ukrainian thousands");
check(Counts.format("100")==null,"plain number untouched");
check(Counts.format("100.8K")==null,"already short untouched");
check(Counts.format("12:30")==null,"time untouched");
check(Counts.format("5 мин")==null,"other words untouched");
check(Counts.format(null)==null,"null untouched");
TextView v=new TextView();v.text="45,2 тыс.";Counts.shorten(v);
check("45,2K".equals(v.text),"shortened");
Counts.shorten(v);check("45,2K".equals(v.text),"left alone");
v.text="45,2 тыс.";Counts.shorten(v);check("45,2K".equals(v.text),"written again shortened again");
v.text="7 млн";Counts.shorten(v);check("7M".equals(v.text),"new count shortened");
}}'''
  })
  with tempfile.TemporaryDirectory() as folder:
   room=pathlib.Path(folder)
   for name,text in sources.items():p=room/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
   out=subprocess.run(['javac','-encoding','UTF-8','-d',folder]+[str(room/n) for n in sources],capture_output=True,text=True);self.assertEqual(0,out.returncode,out.stderr)
   out=subprocess.run(['java','-cp',folder,'com.neverocheg.elite.Test'],capture_output=True,text=True,timeout=30);self.assertEqual(0,out.returncode,out.stderr+out.stdout)
