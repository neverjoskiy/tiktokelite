package com.neverocheg.elite;
import android.content.Context;import android.graphics.Typeface;import android.util.TypedValue;import android.view.Gravity;import android.view.View;import android.widget.LinearLayout;import android.widget.TextView;import java.util.List;
/**
 * Download chooser, kept short so it does not fill the screen.
 * A video post: the tallest size stands alone, filled with the accent and marked as the best; the other
 * sizes follow as one segmented group, the shortest marked as the worst -- both read from what this post
 * really offers. A photo post: a grid of the photos themselves, each with a tick; the button below saves
 * the ticked ones, or every photo while none is ticked.
 * The soundtrack is always the last row.
 */
final class DownloadSheet {
 interface Pick {void on(DownloadQuality.Choice choice);}
 interface Picks {void on(List<DownloadQuality.Choice> choices);}
 private DownloadSheet(){}
 static void show(Context c,List<DownloadQuality.Choice> videos,List<DownloadQuality.Choice> photos,boolean nativeOnly,final Panel.Click nativeSave,final Pick video,final Picks photo,final Panel.Click audio){
  final Skin skin=Skin.remembered(c);
  final boolean photoPost=photos!=null&&!photos.isEmpty();
  final Panel panel=Panel.with(c,skin,photoPost?Text.SHEET_POST_TITLE:Text.SHEET_PICK_QUALITY).compact();
  DownloadQuality.Choice best=null;java.util.ArrayList<DownloadQuality.Choice> rest=new java.util.ArrayList<>();
  if(videos!=null)for(DownloadQuality.Choice v:videos){if(v.best&&best==null)best=v;else rest.add(v);}
  // the "best" entry is the tallest size under another name: do not list that size twice
  java.util.ArrayList<DownloadQuality.Choice> list=new java.util.ArrayList<>();
  if(best!=null)list.add(best);
  for(DownloadQuality.Choice v:rest){boolean same=best!=null&&best.height>0&&v.height==best.height;if(!same)list.add(v);}
  final TextView[] save={null};final java.util.TreeSet<Integer> picked=new java.util.TreeSet<>();
  if(photoPost){
   if(photos.size()==1){panel.hero("image",Text.SAVE_IMAGE,null,"download",()->photo.on(photos));}
   else panel.view(PhotoGrid.make(c,skin,photos,picked,()->{if(save[0]!=null)save[0].setText(picked.isEmpty()?Text.allPhotos(photos.size()):Text.pickedPhotos(picked.size()));}));
  }else if(!list.isEmpty()){
   for(int i=0;i<list.size();i++){
    final DownloadQuality.Choice item=list.get(i);
    String chip=list.size()<2?null:i==0?Text.SHEET_BEST:i==list.size()-1?Text.SHEET_WORST:null;
    String title=item.best?(item.height>0?item.height+"p":"Лучшее"):item.label;
    String icon=item.height>0&&item.height<720?"sd":(item.height<=0&&!item.best?"sd":"hd");
    if(i==0)panel.hero(icon,title,chip,"download",()->video.on(item));
    else panel.item(icon,title,chip,"download",()->video.on(item));
   }
  }else if(nativeOnly){
   panel.hero("hd",Text.DL_NATIVE,null,"download",()->nativeSave.on());
  }
  // no heading: the row says what it is, and a heading only costs another line of height
  panel.section(null);
  panel.item("music_note",Text.DL_AUDIO,null,"download",()->audio.on());
  if(photoPost&&photos.size()>1){
   // the button stays put below the grid however far the photos scroll
   save[0]=panel.action(Text.allPhotos(photos.size()),true);
   save[0].setOnClickListener(v->{
    java.util.ArrayList<DownloadQuality.Choice> chosen=new java.util.ArrayList<>();
    if(picked.isEmpty())chosen.addAll(photos);else for(int i:picked)chosen.add(photos.get(i));
    panel.close();photo.on(chosen);
   });
  }
  panel.quiet(Text.CLOSE,null).show();
 }
 private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
