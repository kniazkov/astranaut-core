package com.kniazkov.astranaut.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Regression fixtures from Qulice 0.22.0, including expected diagnostic positions. */
class LegacyChecksTest {
    static Stream<Path> cases() throws Exception {
        try (var paths = Files.list(Path.of("src/test/resources/checks"))) {
            return paths.sorted().toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("cases")
    void acceptsValid(Path dir) throws Exception {
        Assertions.assertEquals(List.of(), inspect(dir, "Valid.java"), dir.toString());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void rejectsInvalid(Path dir) throws Exception {
        var errors = inspect(dir, "Invalid.java");
        Assertions.assertFalse(errors.isEmpty(), dir.toString());
        for (String line : Files.readAllLines(dir.resolve("violations.txt"))) {
            if (line.isBlank()) continue;
            var split = line.split(":", 2);
            String expected = split[0] + ":" + split[1].trim();
            Assertions.assertTrue(errors.stream().anyMatch(e -> e.contains(expected)),
                () -> dir + " missing " + expected + " in " + errors);
        }
    }

    private List<String> inspect(Path dir, String file) throws Exception {
        var errors = new ArrayList<String>();
        var checker = new Checker();
        checker.setModuleClassLoader(getClass().getClassLoader());
        checker.configure(ConfigurationLoader.loadConfiguration(dir.resolve("config.xml").toString(),
            new PropertiesExpander(new Properties())));
        checker.addListener(new AuditListener() {
            public void auditStarted(AuditEvent e) { }
            public void auditFinished(AuditEvent e) { }
            public void fileStarted(AuditEvent e) { }
            public void fileFinished(AuditEvent e) { }
            public void addError(AuditEvent e) { errors.add(e.getLine() + ":" + e.getMessage()); }
            public void addException(AuditEvent e, Throwable cause) { throw new AssertionError(cause); }
        });
        try { checker.process(List.of(dir.resolve(file).toFile())); }
        finally { checker.destroy(); }
        return errors;
    }
}
