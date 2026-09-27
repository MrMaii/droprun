package app.droprun;

/** Product copy only. User material and reports never pass through a translator. */
final class L {
    private static volatile boolean chinese;
    private L() {}
    static void language(String language) { chinese = "zh".equals(language); }
    static boolean chinese() { return chinese; }
    static String t(String english, String chineseText) { return chinese ? chineseText : english; }
}
