package com.neverocheg.elite;
import android.view.View;
import java.util.Map;
import java.util.WeakHashMap;
/** Native right-area slots only. No frame callbacks, tree walks or removed views. */
public final class FeedRail {
    public static final String[] KEYS={"rail_show_like","rail_show_comment","rail_show_fav","rail_show_share"};
    private static final Map<View,State> slots=new WeakHashMap<View,State>();
    private static volatile boolean[] shown;
    private static final class State {final int kind;int wanted;State(int k,int v){kind=k;wanted=v;}}
    private FeedRail(){}
    public static boolean shown(int kind){
        boolean[] current=shown;
        if(current==null){
            android.content.Context c=Margy.context();if(c==null)return true;
            current=new boolean[]{true,true,true,true};
            try{android.content.SharedPreferences p=AccountAppearance.prefs(c);for(int i=0;i<4;i++)current[i]=p.getBoolean(KEYS[i],true);}
            catch(Throwable error){Diary.note("feed rail settings: "+error);}
            shown=current;
        }
        return kind<0 || kind>=current.length || current[kind];
    }
    public static void setShown(int kind,boolean value){
        if(kind<0 || kind>=4)return;
        android.content.Context c=Margy.context();if(c==null)return;
        AccountAppearance.prefs(c).edit().putBoolean(KEYS[kind],value).apply();reloadSettings();
    }
    static void reloadSettings(){
        shown=null;
        java.util.List<View> views;
        synchronized(slots){views=new java.util.ArrayList<View>(slots.keySet());}
        for(View v:views)if(v!=null)v.post(()->apply(v));
    }
    public static void bindNative(Object component){
        if(component==null)return;
        String name=component.getClass().getName();int kind;
        if(name.equals("com.ss.android.ugc.aweme.feed.assem.digg.VideoDiggAssem"))kind=0;
        else if(name.equals("com.ss.android.ugc.aweme.feed.assem.videocomment.VideoCommentAssem"))kind=1;
        else if(name.equals("com.ss.android.ugc.aweme.feed.favorite.VideoFavoriteAssem"))kind=2;
        else if(name.equals("com.ss.android.ugc.aweme.feed.assem.share.VideoShareAssem"))kind=3;
        else return;
        try{
            Object content=component.getClass().getMethod("getContentView").invoke(component);
            if(!(content instanceof View))return;
            View v=(View)content;
            synchronized(slots){if(!slots.containsKey(v))slots.put(v,new State(kind,v.getVisibility()));}
            apply(v);
            if(kind==3)VideoActions.bindNative(component);
            if(kind==2)Repost.anchor(component);
        }catch(Throwable error){Diary.note("feed rail binding: "+error);}
    }
    /** Preserve TikTok's latest intended state, including server-hidden actions. */
    public static void setVisibility(View view,int visibility){
        if(view==null)return;
        long probe=Probe.start();
        boolean taken=Repost.visibility(view,visibility);
        State state=null;
        if(!taken)synchronized(slots){state=slots.get(view);if(state!=null)state.wanted=visibility;}
        Probe.stop(Probe.RAIL,probe);
        if(taken)return;
        view.setVisibility(state!=null&&state.kind==2&&Repost.replacing(view)?View.INVISIBLE:state!=null&&!shown(state.kind)?View.GONE:visibility);
    }
    static void refreshFavorite(View view){if(view!=null)apply(view);}
    private static void apply(View view){
        State state;synchronized(slots){state=slots.get(view);}
        if(state==null)return;
        int next=state.kind==2&&Repost.replacing(view)?View.INVISIBLE:shown(state.kind)?state.wanted:View.GONE;
        if(view.getVisibility()!=next)view.setVisibility(next);
    }
}
