#!/usr/bin/env python3
"""Exercise Maven rejection paths after clean install; restore every edited file."""
import os
from pathlib import Path
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
LOGS = ROOT / 'target/build-gates'
LOGS.mkdir(parents=True, exist_ok=True)
MAVEN = os.environ.get('MAVEN', 'mvn')


def rejects(name, file, transform, goals, diagnostic):
    original = file.read_bytes()
    try:
        file.write_text(transform(original.decode()), encoding='utf-8')
        result = subprocess.run([MAVEN, '-B', '-ntp', '-pl', ':astranaut-core', *goals],
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (LOGS / f'{name}.log').write_bytes(result.stdout)
        output = result.stdout.decode(errors='replace')
        assert result.returncode != 0, f'{name}: violation was accepted'
        assert diagnostic in output, f'{name}: failed for an unrelated reason; see log'
        print(f'PASS: {name} rejects the deliberate violation', flush=True)
    finally:
        file.write_bytes(original)


jar = ROOT / 'target/astranaut-core-0.0.1.jar'
with zipfile.ZipFile(jar) as archive:
    info = archive.read('com/kniazkov/astranaut/core/Info.class')
    assert int.from_bytes(info[6:8], 'big') == 65, 'Expected Java 21 bytecode'
    assert b'0.0.1' in info
    assert archive.read('com/kniazkov/astranaut/core/VERSION.txt').strip() == b'0.0.1'
    assert not any('checkstyle' in name or 'qulice' in name for name in archive.namelist())
    props = archive.read('META-INF/maven/com.kniazkov/astranaut-core/pom.properties')
    assert b'version=0.0.1' in props
for classifier in ('sources', 'javadoc'):
    assert (jar.parent / f'astranaut-core-0.0.1-{classifier}.jar').is_file()
report = ET.parse(ROOT / 'target/site/jacoco/jacoco.xml')
assert any(int(c.attrib['covered']) > 0 for c in report.findall('counter'))
print('PASS: artifact coordinates, Java 21, attached documentation and coverage', flush=True)

source = ROOT / 'src/main/java/com/kniazkov/astranaut/core/Info.java'
rejects('checkstyle', source, lambda s: s.replace('public final class Info {', 'public final class Info {  '),
        ['checkstyle:check'], 'Line has trailing spaces')
rejects('pmd', source, lambda s: s.replace('public final class Info {',
        'public final class Info {\n    public static void forbidden() { }'),
        ['pmd:check', '-Dpmd.analysisCache=false'], 'ProhibitPublicStaticMethods')
rejects('dependencies', ROOT / 'build-support/library/pom.xml', lambda s: s.replace('<dependencies>',
        '<dependencies><dependency><groupId>commons-codec</groupId><artifactId>commons-codec</artifactId>'
        '<version>1.17.1</version></dependency>', 1),
        ['dependency:analyze-only'], 'Unused declared dependencies')

# Two local dependencies containing different bytes at the same classpath resource.
for label in ('alpha', 'bravo'):
    with zipfile.ZipFile(LOGS / f'{label}.jar', 'w') as archive:
        archive.writestr('build-gate-collision.txt', label)
dependencies = ''.join(
    '<dependency><groupId>build.gate</groupId><artifactId>' + label
    + '</artifactId><version>1</version><scope>system</scope><systemPath>'
    + str(LOGS / f'{label}.jar') + '</systemPath></dependency>'
    for label in ('alpha', 'bravo')
)
rejects('duplicates', ROOT / 'build-support/library/pom.xml',
        lambda s: s.replace('<dependencies>', '<dependencies>' + dependencies, 1),
        ['duplicate-finder:check'], 'Found duplicate')

probe = ROOT / 'src/test/java/BuildGateFailureTest.java'
assert not probe.exists()
probe.write_text('class BuildGateFailureTest { @org.junit.jupiter.api.Test '
                 'void rejectsFailure() { org.junit.jupiter.api.Assertions.fail("build-gate-probe"); } }\n')
try:
    rejects('tests', probe, lambda s: s,
            ['test', '-Dtest=BuildGateFailureTest'], 'build-gate-probe')
finally:
    probe.unlink()
