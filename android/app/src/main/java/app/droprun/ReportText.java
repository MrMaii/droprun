package app.droprun;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;

final class ReportText {
    static CharSequence render(String markdown) {
        String plain=markdown.replaceAll("\\[([^\\]]+)\\]\\(([^\\n]+?)\\)","$1\n$2").replace("`","").replace("**","");
        SpannableStringBuilder result=new SpannableStringBuilder();
        for(String line:plain.split("\n",-1)) {
            boolean heading=line.matches("^#{1,6} .*" );
            String text=heading?line.replaceFirst("^#{1,6} +",""):line.replaceFirst("^- +","• ");
            int start=result.length();result.append(text).append('\n');
            if(heading){result.setSpan(new StyleSpan(Typeface.BOLD),start,start+text.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);result.setSpan(new RelativeSizeSpan(1.15f),start,start+text.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);}
        }
        return result;
    }
}
