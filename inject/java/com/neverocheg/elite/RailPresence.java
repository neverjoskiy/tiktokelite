package com.neverocheg.elite;
import android.view.View;
/** Whether a post is on the screen, told by the buttons beside it. */
final class RailPresence {
    private RailPresence(){}
    /**
     * Whether the column of like, comment, favourite and share buttons is on this screen now.
     *
     * The buttons are the ones TikTok itself hands the mod as it binds a post, so this is
     * not a guess from words or sizes: a screen with a post playing has them, and a settings
     * page, search results or a profile do not. A post scrolled off to the side or still
     * waiting below in the feed has them too, but outside the window, and does not count.
     * A button the user hid in the mod's settings is gone from the layout, so for that one
     * the column it sits in is asked instead.
     */
    static boolean onScreen(View root){
        if(root==null)return false;
        int width=root.getWidth(),height=root.getHeight();if(width<=0||height<=0)return false;
        java.util.List<View> views=FeedRail.views();
        android.graphics.Rect seen=new android.graphics.Rect();
        for(View v:views){
            if(v==null||!v.isAttachedToWindow()||v.getRootView()!=root)continue;
            View probe=v;
            if(v.getVisibility()!=View.VISIBLE&&v.getParent() instanceof View)probe=(View)v.getParent();
            if(!probe.isShown()||probe.getAlpha()<=0.05f)continue;
            if(!probe.getGlobalVisibleRect(seen)||seen.width()<=0||seen.height()<=0)continue;
            // the column stands at the right of the post, wholly inside the window
            if(seen.centerX()<width*.5f||seen.right>width+2||seen.left<0||seen.bottom>height+2||seen.top<0)continue;
            return true;
        }
        return false;
    }
}
