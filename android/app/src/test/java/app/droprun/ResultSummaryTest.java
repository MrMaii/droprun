package app.droprun;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResultSummaryTest {
    @Test public void outcomePrecedesSourceAnalysis(){assertEquals("Updated the card spacing.",TaskPresentation.resultSummary("# A result\n## What I saw\nSource details.\n## What changed\nUpdated the card spacing.\n## Verification\nTests pass."));}
    @Test public void chineseOutcomeAndMachineEvidence(){assertEquals("调整了首页间距。",TaskPresentation.resultSummary("## 我看到了什么\n参考材料\n## 我做了什么\n调整了首页间距。\n## 验证\n构建通过\n```droprun\n{\"outcome\":\"completed\"}\n```"));}
    @Test public void fallbackKeepsFirstHumanParagraph(){assertEquals("A sample, not a real execution.",TaskPresentation.resultSummary("## Demonstration report\nA sample, not a real execution.\n\nMore details."));}

    @Test public void listItemsKeepTheirLinesAndNormalizeInlineSpace(){assertEquals("- First result\n- Second result",TaskPresentation.resultSummary("## What changed\n- First  result\n- Second\t result\n\nDetails stay in the full report."));}
    @Test public void crlfAndStandaloneCrKeepResultLines(){assertEquals("- 第一项\n- 第二项",TaskPresentation.resultSummary("## 交付结果\r\n- 第一项\r- 第二项\r\n\r\n完整报告继续保留。"));}
    @Test public void summaryBoundAndOtherClipBehaviorStaySeparate(){String line="x".repeat(210);assertEquals(line.substring(0,200)+"…",TaskPresentation.resultSummary(line));assertEquals("one two",TaskPresentation.clip("one\n two",200));}
}
