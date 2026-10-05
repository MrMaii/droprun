package app.droprun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * DropRun design system. Light translucent surfaces, a lime accent and a full dark palette.
 * Everything is built in code: no XML layouts,
 * no AndroidX. Animations respect the system "remove animations" setting through {@link #motionEnabled}.
 */
public final class Ui {
    // ---- tokens -------------------------------------------------------------------------------
    public static int BG=0xFFF7F7F2, SURFACE=0xFFFFFFFF, SURFACE_2=0xFFF0F1EC, SURFACE_3=0xFFE7EBDD;
    public static int LINE=0x0C191D1A, LINE_STRONG=0x24191D1A;
    public static int TEXT=0xFF191D1A, MUTED=0xFF616A63, DIM=0xFF687168;
    public static final int LIME=0xFFB8EF73, ON_LIME=0xFF172510;
    public static int LIME_SOFT=0xFFE5F3D7, LIME_LINE=0xFF71894D, ACCENT=0xFF365A24;
    public static int DANGER=0xFFAB342E, AMBER=0xFF826013, SCRIM=0x660F2018;
    public static final int RADIUS=18, RADIUS_CARD=26, RADIUS_SHEET=34;
    static boolean dark;
    private Ui(){}

    public static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    public static float dpf(Context context,float value){return value*context.getResources().getDisplayMetrics().density;}
    public static Typeface regular(){return Typeface.create("sans-serif",Typeface.NORMAL);}
    public static Typeface medium(){return Build.VERSION.SDK_INT>=28?Typeface.create(regular(),600,false):Typeface.create("sans-serif-medium",Typeface.NORMAL);}
    public static boolean motionEnabled(Context context){return ValueAnimator.areAnimatorsEnabled();}
    static String preferenceKey(Context context){android.content.SharedPreferences p=context.getSharedPreferences("droprun.preferences",Context.MODE_PRIVATE);return p.getString("appearance","light")+":"+p.getString("language","en")+":"+(context.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK);}
    static void prepare(Activity activity,boolean overlay){
        android.content.SharedPreferences p=activity.getSharedPreferences("droprun.preferences",Context.MODE_PRIVATE);String mode=p.getString("appearance","light");
        dark=mode.equals("dark")||(mode.equals("system")&&(activity.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);
        L.language(p.getString("language","en"));
        BG=dark?0xFF111512:0xFFF7F7F2;SURFACE=dark?0xFF202521:0xFFFFFFFF;SURFACE_2=dark?0xFF2B312C:0xFFF0F1EC;SURFACE_3=dark?0xFF394339:0xFFE7EBDD;
        TEXT=dark?0xFFF4F5EF:0xFF191D1A;MUTED=dark?0xFFBEC6BD:0xFF616A63;DIM=dark?0xFFA8B3A7:0xFF687168;
        LINE=dark?0x18FFFFFF:0x0C191D1A;LINE_STRONG=dark?0x40FFFFFF:0x24191D1A;LIME_SOFT=dark?0xFF2D4224:0xFFEAF5DD;LIME_LINE=dark?0xFF90B963:0xFF71894D;ACCENT=dark?LIME:0xFF365A24;
        DANGER=dark?0xFFFFB4A9:0xFFAB342E;AMBER=dark?0xFFEBD08A:0xFF826013;SCRIM=dark?0x99000000:0x660F2018;
        activity.setTheme(overlay?(dark?R.style.ShareThemeDark:R.style.ShareTheme):(dark?R.style.AppThemeDark:R.style.AppTheme));
    }
    public static android.graphics.drawable.Drawable ground(){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,dark?new int[]{0xFF171D18,BG,BG}:new int[]{0xFFFCFCF8,BG,0xFFF2F3ED});}

    // ---- window ---------------------------------------------------------------------------------
    /** Palette-aware system bars, drawn edge to edge. */
    public static void configureWindow(Activity activity){
        prepare(activity,false);
        Window window=activity.getWindow();
        window.setStatusBarColor(BG);window.setNavigationBarColor(BG);
        window.setBackgroundDrawable(new ColorDrawable(BG));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        edgeToEdge(window);
    }
    /** Transparent window for the share overlay: the calling app stays visible behind the sheet. */
    public static void configureOverlay(Activity activity){
        prepare(activity,true);
        Window window=activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);window.setNavigationBarColor(Color.TRANSPARENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        edgeToEdge(window);
    }
    private static void edgeToEdge(Window window){
        window.getDecorView();
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller=window.getInsetsController();
            if(controller!=null)controller.setSystemBarsAppearance(dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
    }
    /** Pads a root view with the system bars, display cutout and (optionally) the keyboard. */
    public static void applyInsets(View view,boolean keyboard){
        view.setOnApplyWindowInsetsListener((target,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                int types=WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout();if(keyboard)types|=WindowInsets.Type.ime();
                Insets safe=insets.getInsets(types);target.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }else target.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        view.requestApplyInsets();
    }

    // ---- roots ----------------------------------------------------------------------------------
    /** Scrolling page. Returns the content column (20dp side padding). */
    public static LinearLayout page(Activity activity){
        ScrollView scroll=new ScrollView(activity){
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect){
                int delta=super.computeScrollDeltaToGetChildRectOnScreen(rect);
                // Framework focus scrolling uses the full height, including our system-bar padding.
                if(getChildCount()==0||rect.height()>getHeight()-getPaddingTop()-getPaddingBottom())return delta;
                int target=getScrollY()+delta;
                target=Math.max(target,rect.bottom-getHeight()+getPaddingBottom());
                target=Math.min(target,rect.top-getPaddingTop());
                int limit=Math.max(0,getChildAt(0).getBottom()+getPaddingBottom()-getHeight());
                return Math.max(0,Math.min(target,limit))-getScrollY();
            }
        };scroll.setFillViewport(true);scroll.setClipToPadding(true);scroll.setBackground(ground());scroll.setVerticalScrollBarEnabled(false);
        LinearLayout column=new LinearLayout(activity);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(activity,20),dp(activity,6),dp(activity,20),dp(activity,28));
        scroll.addView(column,new ViewGroup.LayoutParams(-1,-2));activity.setContentView(scroll);applyInsets(scroll,true);
        return column;
    }
    /** Non-scrolling vertical root, for screens that own their own list (header fixed, list scrolls). */
    public static LinearLayout column(Activity activity){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(ground());
        activity.setContentView(root);applyInsets(root,true);return root;
    }
    /** Free-form root for overlays and camera screens. */
    public static FrameLayout frame(Activity activity,boolean opaque){
        FrameLayout root=new FrameLayout(activity);if(opaque)root.setBackgroundColor(BG);
        activity.setContentView(root);applyInsets(root,true);return root;
    }
    /** Bottom sheet surface with rounded top corners, used by the share overlay. */
    public static LinearLayout sheet(Context context){
        LinearLayout sheet=new LinearLayout(context);sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(context,20),dp(context,12),dp(context,20),dp(context,20));
        // Without cross-window blur, an opaque sheet prevents source-app text bleeding through.
        float r=dpf(context,RADIUS_SHEET);GradientDrawable shape=new GradientDrawable();shape.setColor(SURFACE|0xFF000000);shape.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});shape.setStroke(dp(context,1),LINE);
        sheet.setBackground(shape);sheet.setElevation(dpf(context,18));sheet.setClickable(true);sheet.setFocusable(false);sheet.setClipToOutline(true);return sheet;
    }

    // ---- shapes ---------------------------------------------------------------------------------
    public static GradientDrawable outlined(Context context,int fill,int stroke,int radiusDp,int strokeDp){
        GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(dpf(context,radiusDp));if(strokeDp>0)shape.setStroke(dp(context,strokeDp),stroke);return shape;
    }
    public static GradientDrawable surface(Context context,int fill){return outlined(context,fill,LINE,RADIUS_CARD,1);}
    public static GradientDrawable circle(Context context,int fill,int stroke){GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(fill);if(stroke!=0)shape.setStroke(dp(context,1),stroke);return shape;}

    // ---- text -----------------------------------------------------------------------------------
    public static TextView text(Context context,CharSequence value,int size,int color){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(size);view.setTextColor(color);view.setTypeface(regular());
        view.setIncludeFontPadding(false);view.setLineSpacing(dpf(context,2),1.08f);view.setPadding(0,dp(context,3),0,dp(context,3));return view;
    }
    public static TextView title(Context context,CharSequence value,int size){
        TextView view=text(context,value,size,TEXT);view.setTypeface(medium());view.setLetterSpacing(-0.01f);
        if(Build.VERSION.SDK_INT>=28)view.setAccessibilityHeading(true);return view;
    }
    /** Small section label: muted, tracked, sits above a group. */
    public static TextView label(Context context,CharSequence value){
        TextView view=text(context,value,12,MUTED);view.setTypeface(medium());view.setLetterSpacing(0.05f);view.setPadding(0,dp(context,14),0,dp(context,6));return view;
    }
    public static TextView caption(Context context,CharSequence value){return text(context,value,12,MUTED);}
    public static void oneLine(TextView view){view.setMaxLines(1);view.setEllipsize(TextUtils.TruncateAt.END);}
    /** Small status pill: tinted text on a translucent tint of the same color. */
    public static TextView pill(Context context,CharSequence value,int color){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(11);view.setTextColor(color);view.setTypeface(medium());
        view.setIncludeFontPadding(false);view.setPadding(dp(context,9),dp(context,5),dp(context,9),dp(context,5));
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dpf(context,999));shape.setColor((color&0x00ffffff)|0x1F000000);view.setBackground(shape);return view;
    }
    /** 8dp status dot. */
    public static View dot(Context context,int color,int sizeDp){View view=new View(context);view.setBackground(circle(context,color,0));view.setLayoutParams(new LinearLayout.LayoutParams(dp(context,sizeDp),dp(context,sizeDp)));return view;}

    // ---- layout helpers -------------------------------------------------------------------------
    public static LinearLayout row(Context context){LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);return row;}
    public static LinearLayout vertical(Context context){LinearLayout column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);return column;}
    public static void space(LinearLayout parent,int dp){View gap=new View(parent.getContext());boolean horizontal=parent.getOrientation()==LinearLayout.HORIZONTAL;parent.addView(gap,new LinearLayout.LayoutParams(horizontal?dp(parent.getContext(),dp):1,horizontal?1:dp(parent.getContext(),dp)));}
    public static View divider(Context context){View line=new View(context);line.setBackgroundColor(LINE);line.setLayoutParams(new LinearLayout.LayoutParams(-1,Math.max(1,dp(context,1))));return line;}
    public static LinearLayout.LayoutParams fill(){return new LinearLayout.LayoutParams(-1,-2);}
    public static LinearLayout.LayoutParams grow(){return new LinearLayout.LayoutParams(0,-2,1);}
    public static LinearLayout.LayoutParams square(Context context,int dp){return new LinearLayout.LayoutParams(dp(context,dp),dp(context,dp));}
    public static LinearLayout.LayoutParams margins(Context context,int top,int bottom){LinearLayout.LayoutParams params=fill();params.setMargins(0,dp(context,top),0,dp(context,bottom));return params;}
    /** Card: hairline-bordered surface with 16dp padding. */
    public static LinearLayout card(Context context){LinearLayout card=vertical(context);card.setPadding(dp(context,20),dp(context,20),dp(context,20),dp(context,20));card.setBackground(surface(context,SURFACE));card.setElevation(dpf(context,1));return card;}
    /** A project's own initial gives related history and shares a recognizable anchor. */
    public static String projectInitial(CharSequence name){return name.length()==0?"":new String(Character.toChars(Character.toUpperCase(Character.codePointAt(name,0))));}
    public static TextView projectTile(Context context,CharSequence name){
        TextView tile=text(context,projectInitial(name),17,ACCENT);tile.setTypeface(medium());tile.setGravity(Gravity.CENTER);tile.setPadding(0,0,0,0);tile.setBackground(outlined(context,LIME_SOFT,LINE,15,1));tile.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return tile;
    }
    public static LinearLayout.LayoutParams cardParams(Context context){return margins(context,8,2);}

    // ---- controls -------------------------------------------------------------------------------
    public static void styleInput(EditText view){
        Context context=view.getContext();view.setTextColor(TEXT);view.setHintTextColor(DIM);view.setTextSize(15);view.setTypeface(regular());
        view.setPadding(dp(context,14),dp(context,12),dp(context,14),dp(context,12));view.setMinHeight(dp(context,48));view.setGravity(Gravity.TOP|Gravity.START);
        view.setBackgroundTintList(null);
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},outlined(context,SURFACE,LINE,RADIUS,1));
        states.addState(new int[]{android.R.attr.state_focused},outlined(context,SURFACE_2,LIME_LINE,RADIUS,1));
        states.addState(new int[]{},outlined(context,SURFACE_2,LINE_STRONG,RADIUS,1));view.setBackground(states);
    }
    public static Button button(Context context,CharSequence label,boolean primary){Button button=new Button(context);button.setText(label);styleButton(button,primary);return button;}
    public static void styleButton(Button view,boolean primary){styleAction(view,primary?1:0);}
    public static void styleDanger(Button view){styleAction(view,2);}
    public static void styleGhost(Button view){styleAction(view,3);}
    private static void styleAction(Button view,int kind){
        Context context=view.getContext();view.setAllCaps(false);view.setTextSize(15);view.setTypeface(medium());view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(context,52));view.setMinimumHeight(dp(context,52));view.setPadding(dp(context,18),dp(context,14),dp(context,18),dp(context,14));
        view.setStateListAnimator(null);view.setElevation(kind==1?dpf(context,2):0);
        int fill=kind==1?LIME:kind==2?(dark?0xFF38221F:0xFFFFE8E2):kind==3?Color.TRANSPARENT:SURFACE;
        int stroke=kind==1?LIME:kind==2?0x55FFB0A7:kind==3?Color.TRANSPARENT:LINE_STRONG;
        int pressed=kind==1?0xFFA3D964:kind==2?(dark?0xFF382320:0xFFFAD8CF):kind==3?(dark?0x14FFFFFF:0x0C191D1A):SURFACE_3;
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},outlined(context,SURFACE,LINE,RADIUS,1));
        states.addState(new int[]{android.R.attr.state_pressed},outlined(context,pressed,stroke,RADIUS,1));
        states.addState(new int[]{android.R.attr.state_focused},outlined(context,fill,kind==1?ON_LIME:ACCENT,RADIUS,2));
        states.addState(new int[]{},outlined(context,fill,stroke,RADIUS,kind==3?0:1));view.setBackground(states);
        view.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{DIM,kind==1?ON_LIME:kind==2?DANGER:TEXT}));
        bindPress(view);
    }
    /** Round icon button (settings gear, back chevron, close). Tint follows TEXT. */
    public static ImageButton iconButton(Context context,int icon,CharSequence description){
        ImageButton button=new ImageButton(context);button.setImageResource(icon);button.setImageTintList(ColorStateList.valueOf(TEXT));button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);int pad=dp(context,9);button.setPadding(pad,pad,pad,pad);
        GradientDrawable focused=circle(context,SURFACE_2,ACCENT);focused.setStroke(dp(context,2),ACCENT);
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},circle(context,SURFACE_2,LINE));
        states.addState(new int[]{android.R.attr.state_pressed},circle(context,SURFACE_3,LINE_STRONG));
        states.addState(new int[]{android.R.attr.state_focused},focused);
        states.addState(new int[]{},circle(context,SURFACE,LINE));
        button.setBackground(states);bindPress(button);return button;
    }
    /** Compact text pill button for secondary header actions. */
    public static TextView linkButton(Context context,CharSequence value){TextView view=chip(context,value,false);view.setTextSize(14);view.setPadding(dp(context,12),dp(context,10),dp(context,12),dp(context,10));view.setMinHeight(dp(context,48));return view;}
    /** Selectable chip. */
    public static TextView chip(Context context,CharSequence value,boolean selected){
        TextView view=new TextView(context);view.setText(value);view.setTextSize(13);view.setGravity(Gravity.CENTER);view.setTypeface(medium());
        view.setPadding(dp(context,12),dp(context,10),dp(context,12),dp(context,10));view.setMinHeight(dp(context,48));
        styleChip(view,selected);bindPress(view);view.setFocusable(true);view.setClickable(true);return view;
    }
    public static void styleChip(TextView view,boolean selected){Context context=view.getContext();view.setTextColor(selected?ACCENT:TEXT);view.setBackground(choiceSurface(context,selected?LIME_SOFT:SURFACE_2,selected?LIME_LINE:LINE_STRONG,RADIUS));view.setSelected(selected);}
    private static StateListDrawable choiceSurface(Context context,int fill,int stroke,int radius){
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},outlined(context,fill,stroke,radius,stroke==0?0:1));
        states.addState(new int[]{android.R.attr.state_focused},outlined(context,fill,ACCENT,radius,2));
        states.addState(new int[]{},outlined(context,fill,stroke,radius,stroke==0?0:1));return states;
    }
    /** Wrapping chip container built from rows. */
    public static LinearLayout chipGroup(Context context){return vertical(context);}
    public static void addChip(LinearLayout group,View chip,int perRow){
        LinearLayout row=null;Context context=group.getContext();
        if(group.getChildCount()>0){View last=group.getChildAt(group.getChildCount()-1);if(last instanceof LinearLayout&&((LinearLayout)last).getChildCount()<perRow)row=(LinearLayout)last;}
        if(row==null){row=row(context);LinearLayout.LayoutParams params=fill();params.setMargins(0,0,0,dp(context,8));group.addView(row,params);}
        LinearLayout.LayoutParams params=grow();params.setMarginEnd(dp(context,8));row.addView(chip,params);
    }
    /** Segmented control (one selected index), like the effort picker in the GPT app. */
    public static LinearLayout segmented(Context context,List<String> labels,int selected,IntConsumer onSelect){
        LinearLayout group=row(context);group.setPadding(dp(context,3),dp(context,3),dp(context,3),dp(context,3));group.setBackground(outlined(context,SURFACE_2,LINE,RADIUS,1));
        for(int n=0;n<labels.size();n++){
            final int index=n;TextView item=new TextView(context);item.setText(labels.get(n));item.setTextSize(13);item.setTypeface(medium());item.setGravity(Gravity.CENTER);
            item.setPadding(dp(context,6),dp(context,10),dp(context,6),dp(context,10));item.setMinHeight(dp(context,48));
            boolean on=n==selected;item.setSelected(on);item.setTextColor(on?ACCENT:MUTED);item.setBackground(choiceSurface(context,on?SURFACE:Color.TRANSPARENT,on?LINE_STRONG:0,14));
            item.setClickable(true);item.setFocusable(true);item.setContentDescription(labels.get(n)+(on?L.t(", selected","，已选择"):""));
            item.setOnClickListener(v->onSelect.accept(index));bindPress(item);
            LinearLayout.LayoutParams params=grow();if(n>0)params.setMarginStart(dp(context,3));group.addView(item,params);
        }
        return group;
    }
    /** Option row with title, optional detail and a trailing check when selected (GPT-style picker). */
    public static LinearLayout optionRow(Context context,CharSequence title,CharSequence detail,boolean selected){
        LinearLayout row=row(context);row.setPadding(dp(context,14),dp(context,11),dp(context,12),dp(context,11));row.setBackground(choiceSurface(context,selected?LIME_SOFT:SURFACE_2,selected?LIME_LINE:LINE,RADIUS));
        LinearLayout words=vertical(context);TextView heading=text(context,title,15,selected?ACCENT:TEXT);heading.setTypeface(medium());heading.setPadding(0,0,0,0);words.addView(heading);
        if(detail!=null&&detail.length()>0){TextView sub=text(context,detail,12,MUTED);sub.setPadding(0,dp(context,2),0,0);words.addView(sub);}
        row.addView(words,grow());
        ImageView check=new ImageView(context);check.setImageResource(R.drawable.ic_check);check.setImageTintList(ColorStateList.valueOf(ACCENT));check.setVisibility(selected?View.VISIBLE:View.INVISIBLE);check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(check,square(context,20));
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(title+(selected?L.t(", selected","，已选择"):""));bindPress(row);return row;
    }
    /** Two-line list row: title on top, meta beneath, optional trailing view. Compact (≈56dp). */
    public static LinearLayout listRow(Context context,CharSequence title,CharSequence meta,View trailing){
        LinearLayout row=row(context);row.setPadding(dp(context,4),dp(context,10),dp(context,4),dp(context,10));row.setMinimumHeight(dp(context,52));
        LinearLayout words=vertical(context);TextView heading=text(context,title,15,TEXT);heading.setPadding(0,0,0,0);words.addView(heading);
        if(meta!=null&&meta.length()>0){TextView sub=text(context,meta,12,MUTED);sub.setPadding(0,dp(context,2),0,0);words.addView(sub);}
        row.addView(words,grow());
        if(trailing!=null){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMarginStart(dp(context,10));row.addView(trailing,params);}
        row.setClickable(true);row.setFocusable(true);bindPress(row);return row;
    }
    /** Brand mark: the sticky-note wordmark, clipped to a rounded square. Decorative. */
    public static ImageView brandMark(Context context,int sizeDp){
        ImageView logo=new ImageView(context);logo.setImageResource(R.drawable.brand_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(outlined(context,0xFF111914,0,Math.max(6,sizeDp/4),0));logo.setClipToOutline(true);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return logo;
    }
    /** Header row: optional left action, optional brand mark, title, optional right action. */
    public static LinearLayout topBar(Context context,LinearLayout parent,View left,CharSequence title,boolean brand,View right){
        LinearLayout row=row(context);row.setMinimumHeight(dp(context,56));row.setPadding(0,dp(context,6),0,dp(context,6));
        if(left!=null){row.addView(left,square(context,48));space(row,10);}
        if(brand){row.addView(brandMark(context,28),square(context,28));space(row,10);}
        TextView heading=title(context,title,17);heading.setPadding(0,0,0,0);oneLine(heading);row.addView(heading,grow());
        if(right!=null){space(row,10);row.addView(right,square(context,48));}
        if(parent!=null)parent.addView(row,fill());return row;
    }

    // ---- motion ---------------------------------------------------------------------------------
    public static void bindPress(View view){
        // A foreground ripple also covers keyboard presses and custom clickable rows.
        view.setForeground(new RippleDrawable(ColorStateList.valueOf(dark?0x24FFFFFF:0x181C3522),null,surface(view.getContext(),Color.WHITE)));
        view.setOnTouchListener((target,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                target.animate().cancel();
                boolean down=action==MotionEvent.ACTION_DOWN&&target.isEnabled();
                if(motionEnabled(target.getContext()))target.animate().scaleX(down?0.975f:1f).scaleY(down?0.975f:1f).setDuration(down?120:180).setInterpolator(new DecelerateInterpolator(1.8f)).start();
                else{target.setScaleX(1f);target.setScaleY(1f);}
            }
            return false;
        });
    }
    /** Disclosure changes only its own subtree; polling never restarts this motion. */
    public static LinearLayout disclosure(Context context,String title,View content,boolean open,java.util.function.Consumer<Boolean> change){
        LinearLayout group=vertical(context),header=row(context);
        TextView label=text(context,title,15,TEXT);label.setTypeface(medium());header.addView(label,grow());
        ImageView chevron=new ImageView(context);chevron.setImageResource(R.drawable.ic_chevron_left);chevron.setImageTintList(ColorStateList.valueOf(MUTED));chevron.setRotation(open?-90:180);header.addView(chevron,square(context,20));
        header.setMinimumHeight(dp(context,52));header.setPadding(dp(context,4),0,dp(context,4),0);header.setClickable(true);header.setFocusable(true);bindPress(header);
        header.setContentDescription(title+(open?L.t(", expanded",", 已展开"):L.t(", collapsed",", 已折叠")));
        content.setVisibility(open?View.VISIBLE:View.GONE);group.addView(header,fill());group.addView(content,fill());
        header.setTag(open);
        header.setOnClickListener(v->{boolean show=!Boolean.TRUE.equals(header.getTag());header.setTag(show);change.accept(show);header.setContentDescription(title+(show?L.t(", expanded",", 已展开"):L.t(", collapsed",", 已折叠")));expand(content,show);chevron.animate().rotation(show?-90:180).setDuration(motionEnabled(context)?180:0).start();});
        return group;
    }
    public static void expand(View content,boolean show){
        Object running=content.getTag(R.id.expand_animation);if(running instanceof ValueAnimator)((ValueAnimator)running).cancel();
        ViewGroup.LayoutParams params=content.getLayoutParams();
        if(!motionEnabled(content.getContext())){params.height=-2;content.setLayoutParams(params);content.setVisibility(show?View.VISIBLE:View.GONE);content.setAlpha(1);return;}
        View parent=(View)content.getParent();int width=Math.max(1,parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight());
        int from=content.getVisibility()==View.GONE?0:content.getHeight();content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int to=show?content.getMeasuredHeight():0;content.setVisibility(View.VISIBLE);
        ValueAnimator animator=ValueAnimator.ofInt(from,to);content.setTag(R.id.expand_animation,animator);animator.setDuration(280);animator.setInterpolator(new DecelerateInterpolator(1.8f));
        animator.addUpdateListener(a->{params.height=(int)a.getAnimatedValue();content.setLayoutParams(params);content.setAlpha(show?0.4f+0.6f*a.getAnimatedFraction():1f-a.getAnimatedFraction());});
        animator.addListener(new AnimatorListenerAdapter(){boolean cancelled;@Override public void onAnimationCancel(Animator a){cancelled=true;}@Override public void onAnimationEnd(Animator a){if(cancelled)return;params.height=-2;content.setLayoutParams(params);content.setAlpha(1);content.setVisibility(show?View.VISIBLE:View.GONE);content.setTag(R.id.expand_animation,null);}});animator.start();
    }
    /** Compact settings navigation; values wrap at large font sizes. */
    public static LinearLayout setting(Context context,String title,String value,Runnable action){
        ImageView arrow=new ImageView(context);arrow.setImageResource(R.drawable.ic_chevron_left);arrow.setRotation(180);arrow.setImageTintList(ColorStateList.valueOf(MUTED));arrow.setLayoutParams(square(context,18));
        LinearLayout row=listRow(context,title,value,arrow);row.setContentDescription(title+", "+value);row.setOnClickListener(v->action.run());return row;
    }
    /** Entrance for a freshly built screen: 280ms fade + 6dp rise. */
    public static void enter(View view){
        view.animate().cancel();
        if(!motionEnabled(view.getContext())){view.setAlpha(1f);view.setTranslationY(0f);return;}
        view.setAlpha(0f);view.setTranslationY(dpf(view.getContext(),6));
        view.animate().alpha(1f).translationY(0f).setDuration(280).setInterpolator(new DecelerateInterpolator(1.8f)).start();
    }
    /** Slide a sheet up from the bottom edge. */
    public static void slideUp(View view){
        view.animate().cancel();
        if(!motionEnabled(view.getContext())){view.setAlpha(1f);view.setTranslationY(0f);return;}
        view.setAlpha(0f);view.setTranslationY(dpf(view.getContext(),48));
        view.animate().alpha(1f).translationY(0f).setDuration(300).setInterpolator(new DecelerateInterpolator(1.8f)).start();
    }
    public static void slideDown(View view,Runnable end){
        view.animate().cancel();
        if(!motionEnabled(view.getContext())){if(end!=null)end.run();return;}
        view.animate().alpha(0f).translationY(dpf(view.getContext(),48)).setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(end).start();
    }
    public static void fadeIn(View view,long duration){view.animate().cancel();if(!motionEnabled(view.getContext())){view.setAlpha(1f);return;}view.setAlpha(0f);view.animate().alpha(1f).setDuration(duration).start();}
    /**
     * Replace the single child of a stage with the next step. direction 1 = forward (enters from the
     * right), -1 = back (enters from the left). Old content fades out and is removed.
     */
    public static void swap(FrameLayout stage,View next,int direction){
        Context context=stage.getContext();
        for(int n=stage.getChildCount()-1;n>=0;n--){
            View old=stage.getChildAt(n);old.animate().cancel();
            if(motionEnabled(context)){old.animate().alpha(0f).translationX(-direction*dpf(context,16)).setDuration(140).withEndAction(()->stage.removeView(old)).start();}
            else stage.removeView(old);
        }
        stage.addView(next,new FrameLayout.LayoutParams(-1,-2));
        if(!motionEnabled(context)){next.setAlpha(1f);next.setTranslationX(0f);return;}
        next.setAlpha(0f);next.setTranslationX(direction*dpf(context,24));
        next.animate().alpha(1f).translationX(0f).setDuration(280).setInterpolator(new DecelerateInterpolator(1.8f)).start();
    }
    /** Tiny lime pulse used when something is confirmed inline. */
    public static void pulse(View view){if(!motionEnabled(view.getContext()))return;view.animate().cancel();view.setScaleX(0.92f);view.setScaleY(0.92f);view.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(new DecelerateInterpolator(2f)).start();}

    // ---- step dots ------------------------------------------------------------------------------
    /** Step indicator: N dots, the active one stretches into a lime pill; finished dots stay lime. */
    public static final class Dots extends View {
        final int count;float active=0f;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);ValueAnimator animator;
        public Dots(Context context,int count){super(context);this.count=Math.max(1,count);setContentDescription(L.t("Step 1 of ","第 1 步，共 ")+this.count+L.t(" steps"," 步"));}
        public void setActive(int index,boolean animate){
            float target=Math.max(0,Math.min(count-1,index));setContentDescription(L.t("Step ","第 ")+(index+1)+L.t(" of "," 步，共 ")+count+L.t(" steps"," 步"));
            if(animator!=null)animator.cancel();
            if(!animate||!motionEnabled(getContext())){active=target;invalidate();return;}
            animator=ValueAnimator.ofFloat(active,target);animator.setDuration(280);animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a->{active=(float)a.getAnimatedValue();invalidate();});animator.start();
        }
        @Override protected void onMeasure(int w,int h){
            int d=dp(getContext(),6),wide=dp(getContext(),18),gap=dp(getContext(),6);
            setMeasuredDimension(resolveSize(count*d+(wide-d)+(count-1)*gap+getPaddingLeft()+getPaddingRight(),w),resolveSize(d+getPaddingTop()+getPaddingBottom(),h));
        }
        @Override protected void onDraw(Canvas canvas){
            int d=dp(getContext(),6),wide=dp(getContext(),18),gap=dp(getContext(),6);
            float x=getPaddingLeft(),cy=getPaddingTop()+d/2f;
            for(int i=0;i<count;i++){
                float weight=Math.max(0f,1f-Math.abs(i-active));float width=d+(wide-d)*weight;
                int color=i+0.5f<active?ACCENT:blend(LINE_STRONG,ACCENT,weight);
                paint.setColor(color);canvas.drawRoundRect(new RectF(x,cy-d/2f,x+width,cy+d/2f),d/2f,d/2f,paint);x+=width+gap;
            }
        }
    }
    static int blend(int from,int to,float t){
        t=Math.max(0f,Math.min(1f,t));
        return Color.argb(Math.round(Color.alpha(from)+(Color.alpha(to)-Color.alpha(from))*t),Math.round(Color.red(from)+(Color.red(to)-Color.red(from))*t),Math.round(Color.green(from)+(Color.green(to)-Color.green(from))*t),Math.round(Color.blue(from)+(Color.blue(to)-Color.blue(from))*t));
    }

    // ---- check animation ------------------------------------------------------------------------
    /** Lime ring that draws itself, then a check mark. Call {@link #play(Runnable)} once. */
    public static final class CheckView extends View {
        final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG),fillPaint=new Paint(Paint.ANTI_ALIAS_FLAG);float progress=0f;
        public CheckView(Context context){
            super(context);ring.setStyle(Paint.Style.STROKE);ring.setStrokeWidth(dpf(context,3));ring.setColor(ACCENT);ring.setStrokeCap(Paint.Cap.ROUND);ring.setStrokeJoin(Paint.Join.ROUND);
            fillPaint.setColor(LIME_SOFT);setContentDescription(L.t("Allowed","已授权"));
        }
        public void play(Runnable end){
            if(!motionEnabled(getContext())){progress=1f;invalidate();if(end!=null)postDelayed(end,120);return;}
            ValueAnimator animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(720);animator.setInterpolator(new DecelerateInterpolator(1.3f));
            animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});
            animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(end!=null)end.run();}});animator.start();
        }
        @Override protected void onMeasure(int w,int h){int size=dp(getContext(),72);setMeasuredDimension(resolveSize(size,w),resolveSize(size,h));}
        @Override protected void onDraw(Canvas canvas){
            float size=Math.min(getWidth(),getHeight()),inset=ring.getStrokeWidth();float ox=(getWidth()-size)/2f,oy=(getHeight()-size)/2f;
            RectF box=new RectF(ox+inset,oy+inset,ox+size-inset,oy+size-inset);
            float ringPortion=Math.min(1f,progress/0.55f);
            fillPaint.setAlpha(Math.round(0x2E*ringPortion));canvas.drawOval(box,fillPaint);
            canvas.drawArc(box,-90f,360f*ringPortion,false,ring);
            if(progress>0.5f){
                float p=Math.min(1f,(progress-0.5f)/0.5f);
                Path check=new Path();check.moveTo(ox+size*0.30f,oy+size*0.52f);check.lineTo(ox+size*0.44f,oy+size*0.66f);check.lineTo(ox+size*0.71f,oy+size*0.37f);
                PathMeasure measure=new PathMeasure(check,false);Path segment=new Path();measure.getSegment(0f,measure.getLength()*p,segment,true);canvas.drawPath(segment,ring);
            }
        }
    }

    // ---- paper plane ----------------------------------------------------------------------------
    /** A paper plane flies along a curve from the left edge to the right edge, leaving a dashed trail. */
    public static final class PlaneView extends View {
        float progress=0f;final Paint trail=new Paint(Paint.ANTI_ALIAS_FLAG),plane=new Paint(Paint.ANTI_ALIAS_FLAG),fold=new Paint(Paint.ANTI_ALIAS_FLAG),glow=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path route=new Path();PathMeasure measure;final float[] pos=new float[2],tan=new float[2];
        public PlaneView(Context context){
            super(context);trail.setStyle(Paint.Style.STROKE);trail.setStrokeWidth(dpf(context,2));trail.setColor(0x80B8EF73);trail.setStrokeCap(Paint.Cap.ROUND);
            trail.setPathEffect(new DashPathEffect(new float[]{dpf(context,5),dpf(context,6)},0f));
            plane.setColor(LIME);plane.setStyle(Paint.Style.FILL);fold.setColor(0x660B1409);fold.setStyle(Paint.Style.STROKE);fold.setStrokeWidth(dpf(context,1.5f));glow.setColor(LIME);
            setContentDescription(L.t("Handoff saved on your phone","交办已保存在手机"));
        }
        public void play(long duration,Runnable end){
            if(!motionEnabled(getContext())){progress=1f;invalidate();if(end!=null)postDelayed(end,150);return;}
            ValueAnimator animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(duration);animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});
            animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(end!=null)end.run();}});animator.start();
        }
        @Override protected void onMeasure(int w,int h){setMeasuredDimension(resolveSize(dp(getContext(),240),w),resolveSize(dp(getContext(),110),h));}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            route.reset();route.moveTo(w*0.08f,h*0.74f);route.cubicTo(w*0.32f,h*0.02f,w*0.62f,h*1.02f,w*0.92f,h*0.28f);measure=new PathMeasure(route,false);
        }
        @Override protected void onDraw(Canvas canvas){
            if(measure==null)return;float length=measure.getLength(),at=length*progress;
            Path segment=new Path();measure.getSegment(0f,Math.max(1f,at),segment,true);canvas.drawPath(segment,trail);
            measure.getPosTan(at,pos,tan);float angle=(float)Math.toDegrees(Math.atan2(tan[1],tan[0]));
            if(progress>0.82f){float p=(progress-0.82f)/0.18f;glow.setAlpha(Math.round(0x66*(1f-p)));measure.getPosTan(length,pos,tan);canvas.drawCircle(pos[0],pos[1],dpf(getContext(),6+22*p),glow);measure.getPosTan(at,pos,tan);}
            float s=dpf(getContext(),1f);
            canvas.save();canvas.translate(pos[0],pos[1]);canvas.rotate(angle);
            Path body=new Path();body.moveTo(15*s,0);body.lineTo(-11*s,-9*s);body.lineTo(-5*s,0);body.lineTo(-11*s,9*s);body.close();canvas.drawPath(body,plane);
            canvas.drawLine(-5*s,0,15*s,0,fold);canvas.restore();
        }
    }

    // ---- frosted dialog -------------------------------------------------------------------------
    /** Handle for a frosted-glass overlay. Dismiss removes it and clears the blur behind. */
    public static final class Glass {
        public final FrameLayout overlay;public final LinearLayout card;final View behind;final int importance;boolean dismissed=false;
        final View root,previousFocus,surface;final boolean rootFocusable,behindFocusable;final int behindFocusPolicy;
        Glass(FrameLayout overlay,LinearLayout card,View behind){
            this.overlay=overlay;this.card=card;this.behind=behind;surface=overlay.getChildAt(0);root=(View)overlay.getParent();previousFocus=root.findFocus();rootFocusable=root.isFocusable();root.setFocusable(false);
            importance=behind==null?View.IMPORTANT_FOR_ACCESSIBILITY_AUTO:behind.getImportantForAccessibility();behindFocusable=behind!=null&&behind.isFocusable();behindFocusPolicy=behind instanceof ViewGroup?((ViewGroup)behind).getDescendantFocusability():0;
            if(behind!=null){behind.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);behind.setFocusable(false);if(behind instanceof ViewGroup)((ViewGroup)behind).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);}
            overlay.post(()->{if(!dismissed)overlay.requestFocus(View.FOCUS_FORWARD);});
        }
        public void dismiss(Runnable end){
            if(dismissed)return;dismissed=true;
            if(Build.VERSION.SDK_INT>=31&&behind!=null)behind.setRenderEffect(null);
            if(behind!=null)behind.setImportantForAccessibility(importance);
            Runnable remove=()->{ViewGroup parent=(ViewGroup)overlay.getParent();if(parent!=null)parent.removeView(overlay);root.setFocusable(rootFocusable);if(behind!=null){behind.setFocusable(behindFocusable);if(behind instanceof ViewGroup)((ViewGroup)behind).setDescendantFocusability(behindFocusPolicy);}if(previousFocus!=null&&previousFocus.isAttachedToWindow())previousFocus.requestFocus();if(end!=null)end.run();};
            if(!motionEnabled(overlay.getContext())){remove.run();return;}
            surface.animate().cancel();surface.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(140).start();
            overlay.animate().cancel();overlay.animate().alpha(0f).setDuration(160).withEndAction(remove).start();
        }
    }
    /**
     * Frosted-glass dialog over {@code root}. {@code behind} (usually the sheet or page content) gets a
     * real blur on Android 12+, a dim scrim elsewhere. Fill the returned {@link Glass#card} yourself.
     */
    public static Glass glass(Activity activity,FrameLayout root,View behind){
        Context context=activity;
        FrameLayout overlay=new FrameLayout(context);overlay.setBackgroundColor(Build.VERSION.SDK_INT>=31&&behind!=null?0x66000000:0xA6000000);overlay.setClickable(true);overlay.setFocusable(true);
        overlay.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        overlay.setOnApplyWindowInsetsListener((target,insets)->{int bottom=Build.VERSION.SDK_INT>=30?insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()).bottom:insets.getSystemWindowInsetBottom();target.setPadding(0,0,0,Math.max(0,bottom-root.getPaddingBottom()));return insets;});
        LinearLayout card=vertical(context);card.setPadding(dp(context,22),dp(context,22),dp(context,22),dp(context,18));
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,dark?new int[]{0xF0303730,0xFF202521}:new int[]{0xF8FFFFFF,0xFFF3F5EF});shape.setCornerRadius(dpf(context,28));shape.setStroke(dp(context,1),LINE_STRONG);
        if(Build.VERSION.SDK_INT<31)shape.setColors(dark?new int[]{0xFF303730,0xFF202521}:new int[]{0xFFFFFFFF,0xFFF3F5EF});
        card.setClickable(true);
        ScrollView viewport=new ScrollView(context);viewport.addView(card,new ScrollView.LayoutParams(-1,-2));
        FrameLayout surface=new FrameLayout(context);surface.setBackground(shape);surface.setElevation(dpf(context,12));surface.setClipToOutline(true);surface.addView(viewport,new FrameLayout.LayoutParams(-1,-2));
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);int m=dp(context,22);params.setMargins(m,m,m,m);overlay.addView(surface,params);
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        overlay.requestApplyInsets();
        if(Build.VERSION.SDK_INT>=28)overlay.setAccessibilityPaneTitle(L.t("Permission","授权"));
        if(Build.VERSION.SDK_INT>=31&&behind!=null)behind.setRenderEffect(RenderEffect.createBlurEffect(dpf(context,16),dpf(context,16),Shader.TileMode.CLAMP));
        if(motionEnabled(context)){
            overlay.setAlpha(0f);overlay.animate().alpha(1f).setDuration(170).start();
            surface.setAlpha(0f);surface.setScaleX(0.94f);surface.setScaleY(0.94f);surface.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240).setInterpolator(new DecelerateInterpolator(1.8f)).start();
        }
        return new Glass(overlay,card,behind);
    }
}
