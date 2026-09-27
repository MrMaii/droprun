package app.droprun;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared native styling. Translucent layers and highlights, not background blur. */
public final class BrandUi {
    public static final int BG=Color.rgb(17,25,20),CARD=Color.rgb(29,40,33),CARD_SOFT=Color.rgb(24,34,28),TEXT=Color.rgb(245,247,242),MUTED=Color.rgb(170,188,174),GREEN=Color.rgb(184,239,115),DANGER=Color.rgb(255,176,167),AMBER=Color.rgb(255,214,130);
    private BrandUi(){}

    public static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}

    public static void configureWindow(Activity activity){
        Window window=activity.getWindow();
        View decor=window.getDecorView();
        window.setStatusBarColor(BG);window.setNavigationBarColor(BG);
        window.setBackgroundDrawable(background(activity));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller=window.getInsetsController();
            if(controller!=null)controller.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }else decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    /** A scrolling page with safe-area padding. Returns the content column. */
    public static LinearLayout page(Activity activity){
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackground(background(activity));
        LinearLayout column=new LinearLayout(activity);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(activity,20),dp(activity,16),dp(activity,20),dp(activity,28));
        scroll.addView(column);activity.setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30){Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
            else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.requestApplyInsets();
        return column;
    }

    public static Drawable background(Context context){
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff1b2f23,BG,0xff0e1812});
    }

    public static GradientDrawable surface(Context context,int color){
        GradientDrawable drawable=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{tint(color,0.055f),color});
        drawable.setCornerRadius(dp(context,22));drawable.setStroke(dp(context,1),0x405c7a63);return drawable;
    }

    private static int tint(int color,float amount){
        return Color.argb(Color.alpha(color),Math.round(Color.red(color)+(255-Color.red(color))*amount),Math.round(Color.green(color)+(255-Color.green(color))*amount),Math.round(Color.blue(color)+(255-Color.blue(color))*amount));
    }

    private static GradientDrawable outlined(Context context,int color,int stroke,int radius){
        GradientDrawable drawable=surface(context,color);drawable.setCornerRadius(dp(context,radius));drawable.setStroke(dp(context,1),stroke);return drawable;
    }

    public static TextView text(Context context,String value,int size,int color){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));view.setLineSpacing(dp(context,2),1.08f);
        view.setPadding(0,dp(context,4),0,dp(context,6));return view;
    }

    public static TextView title(Context context,String value,int size){
        TextView view=text(context,value,size,TEXT);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        if(Build.VERSION.SDK_INT>=28)view.setAccessibilityHeading(true);return view;
    }

    public static TextView label(Context context,String value){
        TextView view=text(context,value,13,MUTED);view.setLetterSpacing(0.04f);view.setPadding(0,dp(context,14),0,dp(context,4));return view;
    }

    /** Small pill used for status and inline tags. */
    public static TextView pill(Context context,String value,int color){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(12);view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        view.setPadding(dp(context,10),dp(context,4),dp(context,10),dp(context,4));
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(context,999));shape.setColor((color&0x00ffffff)|0x22000000);shape.setStroke(dp(context,1),(color&0x00ffffff)|0x55000000);
        view.setBackground(shape);return view;
    }

    /** Selectable chip for models, efforts and options. */
    public static TextView chip(Context context,String value,boolean selected){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(14);view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        view.setPadding(dp(context,14),dp(context,9),dp(context,14),dp(context,9));view.setMinHeight(dp(context,40));
        styleChip(view,selected);bindPress(view);view.setFocusable(true);view.setClickable(true);
        return view;
    }

    public static void styleChip(TextView view,boolean selected){
        Context context=view.getContext();
        view.setTextColor(selected?BG:TEXT);
        view.setBackground(outlined(context,selected?GREEN:0xe6233227,selected?0xffd6f7b4:0x80637c69,14));
    }

    public static LinearLayout row(Context context){
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);return row;
    }

    /** Wrapping chip container built from rows; avoids a FlexboxLayout dependency. */
    public static LinearLayout chipGroup(Context context){
        LinearLayout group=new LinearLayout(context);group.setOrientation(LinearLayout.VERTICAL);return group;
    }

    public static void addChip(LinearLayout group,View chip,int perRow){
        LinearLayout row=null;
        if(group.getChildCount()>0){View last=group.getChildAt(group.getChildCount()-1);if(last instanceof LinearLayout&&((LinearLayout)last).getChildCount()<perRow)row=(LinearLayout)last;}
        if(row==null){row=row(group.getContext());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,0,0,dp(group.getContext(),8));group.addView(row,params);}
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);params.setMargins(0,0,row.getChildCount()==0?0:0,0);params.setMarginEnd(dp(group.getContext(),8));row.addView(chip,params);
    }

    public static LinearLayout card(Context context){
        LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(context,18),dp(context,14),dp(context,18),dp(context,14));card.setBackground(surface(context,CARD));
        return card;
    }

    public static LinearLayout.LayoutParams cardParams(Context context){
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(context,10),0,dp(context,2));return params;
    }

    /** Dashboard stat tile: a big number over a short label. */
    public static LinearLayout tile(Context context,String value,String label,int color){
        LinearLayout tile=card(context);tile.setGravity(Gravity.CENTER_VERTICAL);
        TextView number=text(context,value,34,color);number.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));number.setPadding(0,0,0,0);
        TextView caption=text(context,label,13,MUTED);caption.setPadding(0,dp(context,2),0,0);
        tile.addView(number);tile.addView(caption);return tile;
    }

    public static void styleInput(EditText view){
        Context context=view.getContext();view.setTextColor(TEXT);view.setHintTextColor(MUTED);view.setTextSize(16);
        view.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));view.setPadding(dp(context,16),dp(context,14),dp(context,16),dp(context,14));
        view.setMinHeight(dp(context,54));view.setGravity(Gravity.TOP|Gravity.START);
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},outlined(context,0xff1b251e,0x405c7a63,17));
        states.addState(new int[]{android.R.attr.state_focused},outlined(context,CARD,GREEN,17));
        states.addState(new int[]{},outlined(context,0xe61d2821,0x705c7a63,17));view.setBackground(states);
    }

    public static void styleButton(Button view,boolean primary){styleAction(view,primary,false);}
    public static void styleDanger(Button view){styleAction(view,false,true);}

    private static void styleAction(Button view,boolean primary,boolean danger){
        Context context=view.getContext();view.setAllCaps(false);view.setTextSize(16);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        view.setGravity(Gravity.CENTER);view.setMinHeight(dp(context,52));view.setMinimumHeight(dp(context,52));
        view.setPadding(dp(context,18),dp(context,14),dp(context,18),dp(context,14));view.setStateListAnimator(null);
        int normal=primary?GREEN:danger?0xff352621:0xe627372c;
        int border=primary?0xffd6f7b4:danger?0xff916457:0x80637c69;
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},outlined(context,0xff28372d,0xff4d6254,17));
        states.addState(new int[]{android.R.attr.state_pressed},outlined(context,primary?0xffa1d764:danger?0xff52332c:0xff394e3e,border,17));
        states.addState(new int[]{android.R.attr.state_focused},outlined(context,normal,primary?TEXT:GREEN,17));
        states.addState(new int[]{},outlined(context,normal,border,17));view.setBackground(states);
        view.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,primary?BG:danger?DANGER:TEXT}));
        bindPress(view);
    }

    /** Compact text button used for the header actions (settings, back). */
    public static TextView linkButton(Context context,String value){
        TextView view=chip(context,value,false);view.setTextSize(14);view.setPadding(dp(context,14),dp(context,8),dp(context,14),dp(context,8));return view;
    }

    public static boolean motionEnabled(Context context){return ValueAnimator.areAnimatorsEnabled();}

    public static void bindPress(View view){
        view.setOnTouchListener((target,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                target.animate().cancel();
                float scale=action==MotionEvent.ACTION_DOWN&&target.isEnabled()&&motionEnabled(target.getContext())?.985f:1f;
                if(motionEnabled(target.getContext()))target.animate().scaleX(scale).scaleY(scale).setDuration(130).setInterpolator(new DecelerateInterpolator()).start();
                else{target.setScaleX(1f);target.setScaleY(1f);}
            }
            return false;
        });
    }

    public static void enter(View view){
        view.animate().cancel();
        if(!motionEnabled(view.getContext())){view.setAlpha(1f);view.setTranslationY(0f);view.setScaleX(1f);view.setScaleY(1f);return;}
        view.setAlpha(0f);view.setTranslationY(dp(view.getContext(),7));
        view.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
    }

    /** Header: logo and name on the left, one optional action on the right. */
    public static LinearLayout header(Activity activity,LinearLayout parent,boolean large,View action){
        LinearLayout row=row(activity);
        ImageView logo=new ImageView(activity);logo.setImageResource(R.drawable.brand_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(surface(activity,BG));logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int size=dp(activity,large?72:48);row.addView(logo,new LinearLayout.LayoutParams(size,size));
        LinearLayout words=new LinearLayout(activity);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(activity,12),0,0,0);
        TextView name=text(activity,"DropRun",large?22:18,TEXT);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));name.setPadding(0,0,0,dp(activity,2));words.addView(name);
        TextView caption=text(activity,"你转发，它开工。",12,MUTED);caption.setPadding(0,0,0,0);words.addView(caption);
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null)row.addView(action,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,0,0,dp(activity,large?16:10));parent.addView(row,params);
        return row;
    }
}
