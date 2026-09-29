package app.droprun;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResultSummaryTest {
    @Test public void outcomePrecedesSourceAnalysis(){assertEquals("Updated the card spacing.",TaskPresentation.resultSummary("# A result\n## What I saw\nSource details.\n## What changed\nUpdated the card spacing.\n## Verification\nTests pass."));}
    @Test public void chineseOutcomeAndMachineEvidence(){assertEquals("调整了首页间距。",TaskPresentation.resultSummary("## 我看到了什么\n参考材料\n## 我做了什么\n调整了首页间距。\n## 验证\n构建通过\n```droprun\n{\"outcome\":\"completed\"}\n```"));}
    @Test public void fallbackKeepsFirstHumanParagraph(){assertEquals("A sample, not a real execution.",TaskPresentation.resultSummary("## Demonstration report\nA sample, not a real execution.\n\nMore details."));}
}
