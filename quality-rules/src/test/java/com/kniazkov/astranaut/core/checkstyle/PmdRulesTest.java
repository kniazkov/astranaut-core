package com.kniazkov.astranaut.core.checkstyle;

import java.util.stream.Stream;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.RuleSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Positive and negative Java 21 fixtures for each migrated XPath rule. */
class PmdRulesTest {
    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of("UnitTestAssertionsShouldIncludeMessage", "class Sample { void helper(){org.junit.jupiter.api.Assertions.assertTrue(true);} }", "class Sample { void helper(){org.junit.Assert.assertTrue(true);} }"),
            Arguments.of("UnitTestContainsTooManyAsserts", "class Sample { @org.junit.jupiter.api.Test void testRun(){org.junit.jupiter.api.Assertions.assertTrue(true);org.junit.jupiter.api.Assertions.assertFalse(false);} }", "class Sample { @org.junit.Test public void testRun(){org.junit.Assert.assertTrue(true);org.junit.Assert.assertFalse(false);} }"),
            Arguments.of("UnnecessaryFullyQualifiedName", "class Sample { static int val; int read(){return Sample.val;} }", "import java.util.List; class Sample { java.util.List<String> values; }"),
            Arguments.of("ConstructorShouldDoInitialization", "class Sample { int val; Sample() { this.val=1; } }", "class Sample { int val=1; Sample() {} }"),
            Arguments.of("OnlyOneConstructorShouldDoInitialization", "class Sample { int val; Sample(){this(1);} Sample(int val){this.val=val;} }", "class Sample { int val; Sample(){this.val=1;} Sample(int val){this.val=val;} }"),
            Arguments.of("ConstructorOnlyInitializesOrCallOtherConstructors", "class Sample { int val; Sample(){this.val=1;} }", "class Sample { Sample(){System.out.println(1);} }"),
            Arguments.of("StaticAccessToStaticFields", "class Sample { static int val; int read(){return Sample.val;} }", "class Sample { static int val; int read(){return val;} }"),
            Arguments.of("ProhibitPublicStaticMethods", "class Sample { public static void main(String... args){} }", "class Sample { public static void run(){} }"),
            Arguments.of("ProhibitFilesCreateFileInTests", "import java.nio.file.*; class SampleTest { void run() throws Exception { Files.createTempFile(\"a\",\"b\"); } }", "import java.nio.file.*; class SampleTest { void run() throws Exception { Files.createFile(Path.of(\"a\")); } }"),
            Arguments.of("ProhibitPlainJunitAssertionsRule", "import org.junit.Assert; class Sample { void run(){Assert.fail();} }", "import org.junit.Assert; class Sample { void run(){Assert.assertTrue(true);} }"),
            Arguments.of("UseStringIsEmptyRule", "class Sample { boolean run(String str){return str.isEmpty();} }", "class Sample { boolean run(String str){return str.length()==0;} }"),
            Arguments.of("AvoidUsingShortType", "class Sample { int val; }", "class Sample { short val; }"),
            Arguments.of("SimplifyStartsWith", "class Sample { boolean run(String str){return str.startsWith(\"a\");} }", "class Sample { boolean run(String str){return str.indexOf(\"a\")==0;} }")
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void preservesRule(String name, String valid, String invalid) {
        Assertions.assertEquals(0, violations(name, valid), name + " rejects valid source");
        Assertions.assertTrue(violations(name, invalid) > 0, name + " misses violation");
    }

    private int violations(String name, String source) {
        var config = new PMDConfiguration();
        config.setIgnoreIncrementalAnalysis(true);
        config.setThreads(1);
        try (var pmd = PmdAnalysis.create(config)) {
            var rules = pmd.newRuleSetLoader().loadFromResource("../config/pmd.xml");
            pmd.addRuleSet(RuleSet.forSingleRule(rules.getRuleByName(name)));
            pmd.files().addSourceFile(FileId.fromPathLikeString("Sample.java"), source);
            var report = pmd.performAnalysisAndCollectReport();
            Assertions.assertTrue(report.getProcessingErrors().isEmpty(), report.getProcessingErrors().toString());
            Assertions.assertTrue(report.getConfigurationErrors().isEmpty(), report.getConfigurationErrors().toString());
            return report.getViolations().size();
        }
    }
}
