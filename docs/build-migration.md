# Build migration baseline

Baseline: core `1015ebfbd0a091882dfaa14435b217665fb76aa4`, Qulice 0.22.0
(tag commit `3cb71f86b18b5d1db018a5d0a491ad46eddc2432`), jcabi parent 0.57.2.

## Existing contract

- Java 8, JUnit Jupiter 5.6.2, Surefire 3.0.0-M5, JaCoCo 0.8.7.
- Qulice executes Checkstyle 8.15 and PMD 6.10.0 over production and test Java.
- Dependency analysis rejects undeclared dependencies and unused compile dependencies;
  `ch.qos.logback:logback-classic` is an intentional runtime provider exception.
- Duplicate Finder rejects classpath conflicts, excludes META-INF and module-info,
  and does not check the test classpath. This is not source-code clone detection.
- Enforcer checks Java/Maven minimums; release versions reject SNAPSHOT dependencies,
  plugins and build extensions. SVN properties do not apply to this Git repository.
- POM XPath assertions are empty, so that validator has no project-specific checks.
- Parent adds build revision/timestamp and package VERSION.txt generation; no core
  implementation imports jcabi to read these. Preserve version information without
  rewriting tracked source directories.
- GitHub Actions runs clean verify then uploads coverage to Codecov with a required
  token. Rultor deploy/release reads settings and signing keys from another account.
- Output: library JAR; release additionally needs sources, Javadoc and signatures.

## Checkstyle rules and explicit properties

The following inventory records the old policy, not a new default ruleset.

- `Checker`: `charset=UTF-8, localeLanguage=en`
- `JavadocPackage`
- `NewlineAtEndOfFile`: `fileExtensions=java, lineSeparator=lf`
- `Translation`
- `FileLength`: `max=1000, fileExtensions=java`
- `FileTabCharacter`: `eachLine=true, fileExtensions=java,txt,xml,xsl,xsd,properties,groovy,vm,MF,sh,sql,tokens,g,spec,css,csv`
- `RegexpSingleline`: `format=\s+$, fileExtensions=java, message=Line has trailing spaces.`
- `RegexpSingleline`: `format=/\*.*\*/, fileExtensions=java, message=This kind of comment is not allowed.`
- `RegexpMultiline`: `format=\r\n$, message=Lines in file should end with Unix-like end of line`
- `RegexpMultiline`: `format=\r?\n[ \t\x0B\f]*\r?\n[ \t\x0B\f]*\r?\n, fileExtensions=java, message=Two consecutive empty lines`
- `RegexpSingleline`: `format=\* +@return +[^A-Z], fileExtensions=java, message=@return tag description should start with capital letter`
- `RegexpSingleline`: `format=\* +@param +\w+ +[^A-Z], fileExtensions=java, message=@param tag description should start with capital letter`
- `RegexpSingleline`: `format=/\*\* +[^A-Z\{], fileExtensions=java, message=First sentence in a comment should start with a capital letter`
- `RegexpMultiline`: `format=/\*\*\W+\* +[^A-Z\{], fileExtensions=java, message=First sentence in a comment should start with a capital letter`
- `RegexpSingleline`: `format=synchronized +\(this\) +\{, fileExtensions=java, message=Using THIS as a lock is a bad practice (use class variable instead)`
- `RegexpSingleline`: `format=synchronized +\([\w\.]\.class\) +\{, fileExtensions=java, message=Using class as a lock is a bad practice (use class variable instead)`
- `RegexpSingleline`: `format=org\.apache\.commons\.(codec|lang3?)\.CharEncoding, fileExtensions=java, message=Use 'java.nio.charset.StandardCharsets' instead`
- `RegexpSingleline`: `format=com\.google\.common\.base\.Charsets, fileExtensions=java, message=Use 'java.nio.charset.StandardCharsets' instead`
- `RegexpSingleline`: `format=^(?! *(/\*\*|\*|/\*|//)).*[\.\-\+%/\*<>](?<!(->)|<*[A-Za-z]>) *$, fileExtensions=java, message=Line cannot end with this symbol, move it to the next line`
- `RegexpSingleline`: `format=^ *=, fileExtensions=java, message=Line cannot start with this symbol, move it to the previous line`
- `Header`: `header=${header}, fileExtensions=java`
- `SuppressionFilter`: `file=/com/qulice/checkstyle/suppressions.xml, optional=false`
- `TreeWalker`: `cacheFile=${cache.file}`
- `SuppressWithNearbyCommentFilter`: `commentFormat=@checkstyle (\w+) \((\d+) lines?\), checkFormat=$1, influenceFormat=$2`
- `AnnotationUseStyle`: `elementStyle=compact_no_array, trailingArrayComma=never, closingParens=ignore`
- `MissingDeprecated`
- `MissingOverride`
- `PackageAnnotation`
- `SuppressWarnings`
- `EmptyBlock`
- `LeftCurly`
- `NeedBraces`
- `RightCurly`
- `AvoidNestedBlocks`
- `VisibilityModifier`
- `FinalClass`
- `InterfaceIsType`
- `HideUtilityClassConstructor`
- `DesignForExtension`
- `MutableException`
- `ThrowsCount`
- `InnerTypeLast`
- `ArrayTrailingComma`
- `AvoidInlineConditionals`
- `CovariantEquals`
- `EmptyStatement`
- `EqualsHashCode`
- `FinalLocalVariable`
- `HiddenField`: `ignoreConstructorParameter=true`
- `IllegalInstantiation`
- `IllegalToken`: `tokens=POST_INC, POST_DEC`
- `IllegalTokenText`
- `InnerAssignment`
- `MissingSwitchDefault`
- `ModifiedControlVariable`
- `SimplifyBooleanExpression`
- `SimplifyBooleanReturn`
- `StringLiteralEquality`
- `NestedForDepth`
- `NestedIfDepth`
- `NestedTryDepth`
- `NoClone`
- `NoFinalizer`
- `SuperClone`
- `SuperFinalize`
- `IllegalCatch`
- `IllegalThrows`
- `PackageDeclaration`
- `ReturnCount`: `max=1`
- `IllegalType`
- `DeclarationOrder`
- `ParameterAssignment`
- `ExplicitInitialization`
- `DefaultComesLast`
- `FallThrough`
- `MultipleVariableDeclarations`
- `RequireThis`: `checkFields=true, checkMethods=true, validateOnlyOverlapping=false`
- `UnnecessaryParentheses`
- `OneStatementPerLine`
- `AvoidStarImport`
- `AvoidStaticImport`
- `IllegalImport`
- `RedundantImport`
- `ImportOrder`
- `NoLineWrap`
- `JavadocType`: `allowUnknownTags=true`
- `JavadocMethod`
- `JavadocVariable`
- `JavadocStyle`
- `AtclauseOrder`: `tagOrder=@param, @return,                 @throws, @exception, @see, @since, @serial, @serialField,                 @serialData, @deprecated, @todo, @checkstyle`
- `BooleanExpressionComplexity`
- `ClassFanOutComplexity`: `max=30`
- `CyclomaticComplexity`
- `NPathComplexity`
- `JavaNCSS`
- `TodoComment`
- `UpperEll`
- `ArrayTypeStyle`
- `FinalParameters`: `tokens=CTOR_DEF,METHOD_DEF,FOR_EACH_CLAUSE,LITERAL_CATCH`
- `Indentation`
- `CommentsIndentation`
- `TrailingComment`
- `OuterTypeFilename`
- `ModifierOrder`
- `RedundantModifier`
- `AbbreviationAsWordInNameCheck`: `allowedAbbreviations=IT, allowedAbbreviationLength=1`
- `ClassTypeParameterName`
- `ConstantName`
- `LocalFinalVariableName`: `format=^(id|[a-z]{3,12})$, tokens=VARIABLE_DEF`
- `LocalVariableName`: `format=^(id|[a-z]{3,12})$`
- `CatchParameterName`: `format=^(ex|[a-z]{3,12})$`
- `MemberName`: `format=^(id|[a-z]{3,12})$`
- `MethodName`: `format=^[a-z]{2,}[a-zA-Z]+$`
- `MethodTypeParameterName`
- `PackageName`
- `ParameterName`: `format=^(id|[a-z]{3,})$`
- `StaticVariableName`
- `TypeName`
- `ExecutableStatementCount`: `max=40`
- `LineLength`: `max=100, ignorePattern=^import .*$|^\s+\*\s.*https?:\/\/.+|^\s+"`
- `AnonInnerLength`
- `MethodLength`
- `ParameterNumber`: `max=3, ignoreOverriddenMethods=true`
- `OuterTypeNumber`
- `MethodCount`
- `GenericWhitespace`
- `EmptyForInitializerPad`
- `EmptyForIteratorPad`
- `MethodParamPad`
- `NoWhitespaceAfter`
- `NoWhitespaceBefore`
- `OperatorWrap`
- `ParenPad`
- `TypecastParenPad`
- `WhitespaceAfter`
- `WhitespaceAround`
- `EmptyLineSeparator`: `allowMultipleEmptyLines=false, allowMultipleEmptyLinesInsideClassMembers=false, tokens=IMPORT,CLASS_DEF,INTERFACE_DEF,ENUM_DEF,STATIC_INIT,INSTANCE_INIT,METHOD_DEF,CTOR_DEF,VARIABLE_DEF`
- `com.qulice.checkstyle.BracketsStructureCheck`
- `com.qulice.checkstyle.CurlyBracketsStructureCheck`
- `com.qulice.checkstyle.EmptyLinesCheck`
- `com.qulice.checkstyle.StringLiteralsConcatenationCheck`
- `com.qulice.checkstyle.MultilineJavadocTagsCheck`
- `com.qulice.checkstyle.NoJavadocForOverriddenMethodsCheck`
- `com.qulice.checkstyle.MethodBodyCommentsCheck`
- `com.qulice.checkstyle.MethodsOrderCheck`
- `com.qulice.checkstyle.JavadocLocationCheck`
- `com.qulice.checkstyle.ConstantUsageCheck`
- `com.qulice.checkstyle.NonStaticMethodCheck`: `excludeFileNamePattern=.*(Test|IT|ITCase)\.java`
- `com.qulice.checkstyle.ProtectedMethodInFinalClassCheck`
- `com.qulice.checkstyle.FinalSemicolonInTryWithResourcesCheck`
- `com.qulice.checkstyle.JavadocEmptyLineCheck`
- `com.qulice.checkstyle.DiamondOperatorCheck`
- `com.qulice.checkstyle.JavadocParameterOrderCheck`
- `com.qulice.checkstyle.JavadocTagsCheck`
- `com.qulice.checkstyle.ImportCohesionCheck`: `fileExtensions=java`
- `com.qulice.checkstyle.CascadeIndentationCheck`: `fileExtensions=java`
- `com.qulice.checkstyle.ConditionalRegexpMultilineCheck`: `condition=^import java\.util\.ArrayList;$, format=new ArrayList(?:<.*?>)?\(\), message=ArrayList should be initialized with a size parameter, fileExtensions=java`
- `RegexpMultiline`: `format=new java.util.ArrayList(?:<.*?>)?\(\), message=ArrayList should be initialized with a size parameter, fileExtensions=java`

## PMD rules

- `category/java/bestpractices.xml`; excludes: `JUnitTestsShouldIncludeAssert`, `PositionLiteralsFirstInComparisons`, `PositionLiteralsFirstInCaseInsensitiveComparisons`
- `category/java/codestyle.xml`; excludes: `AtLeastOneConstructor`, `AvoidFinalLocalVariable`, `ShortClassName`, `ShortVariable`, `AbstractNaming`, `ClassNamingConventions`, `CommentDefaultAccessModifier`, `DefaultPackage`, `LinguisticNaming`, `CallSuperInConstructor`
- `category/java/design.xml`; excludes: `LoosePackageCoupling`, `LawOfDemeter`, `SignatureDeclareThrowsException`, `ExcessiveImports`, `CyclomaticComplexity`
- `category/java/documentation.xml`; excludes: `CommentRequired`, `CommentSize`
- `category/java/errorprone.xml`; excludes: `DataflowAnomalyAnalysis`, `AvoidLiteralsInIfCondition`, `BeanMembersShouldSerialize`, `MissingSerialVersionUID`
- `category/java/performance.xml`; excludes: `AvoidInstantiatingObjectsInLoops`
- `category/java/multithreading.xml`; excludes: `AvoidUsingVolatile`, `UseConcurrentHashMap`, `DoNotUseThreads`
- `category/java/errorprone.xml/AvoidDuplicateLiterals`
- `ProhibitPlainJunitAssertionsRule`
- `UseStringIsEmptyRule`
- `UnnecessaryLocalRule`
- `ConstructorShouldDoInitialization`
- `OnlyOneConstructorShouldDoInitialization`
- `ConstructorOnlyInitializesOrCallOtherConstructors`
- `StaticAccessToStaticFields`
- `ProhibitPublicStaticMethods`
- `ProhibitFilesCreateFileInTests`
- `ReturnEmptyCollectionRatherThanNull`
- `JUnit5TestShouldBePackagePrivate`

## PMD 7 replacements

Mappings preserve the intent; analyzer upgrades can change individual diagnostics.

| Legacy rule | Replacement |
|---|---|
| `AvoidPrefixingMethodParameters` | FormalParameterNamingConventions |
| `AvoidUsingShortType` | XPath AvoidUsingShortType |
| `BadComparison` | ComparisonWithNaN |
| `BooleanInstantiation` | PrimitiveWrapperInstantiation |
| `ByteInstantiation` | PrimitiveWrapperInstantiation |
| `CloneThrowsCloneNotSupportedException` | Checkstyle NoClone |
| `DoNotCallSystemExit` | DoNotTerminateVM |
| `DontImportJavaLang` | UnnecessaryImport |
| `DuplicateImports` | UnnecessaryImport |
| `EmptyFinallyBlock` | EmptyControlStatement |
| `EmptyIfStmt` | EmptyControlStatement |
| `EmptyInitializer` | EmptyControlStatement |
| `EmptyStatementBlock` | EmptyControlStatement |
| `EmptyStatementNotInLoop` | UnnecessarySemicolon |
| `EmptySwitchStatements` | EmptyControlStatement |
| `EmptySynchronizedBlock` | EmptyControlStatement |
| `EmptyTryBlock` | EmptyControlStatement |
| `EmptyWhileStmt` | EmptyControlStatement |
| `ExcessiveClassLength` | Checkstyle FileLength |
| `ExcessiveMethodLength` | Checkstyle MethodLength |
| `ForLoopsMustUseBraces` | ControlStatementBraces |
| `IfElseStmtsMustUseBraces` | ControlStatementBraces |
| `IfStmtsMustUseBraces` | ControlStatementBraces |
| `ImportFromSamePackage` | UnnecessaryImport |
| `IntegerInstantiation` | PrimitiveWrapperInstantiation |
| `InvalidSlf4jMessageFormat` | InvalidLogMessageFormat |
| `JUnit5TestShouldBePackagePrivate` | JUnit5TestShouldBePackagePrivate |
| `LoggerIsNotStaticFinal` | ProperLogger |
| `LongInstantiation` | PrimitiveWrapperInstantiation |
| `MIsLeadingVariableName` | FieldNamingConventions |
| `MissingBreakInSwitch` | ImplicitSwitchFallThrough |
| `ModifiedCyclomaticComplexity` | CyclomaticComplexity |
| `NcssConstructorCount` | NcssCount |
| `NcssMethodCount` | NcssCount |
| `NcssTypeCount` | NcssCount |
| `ReturnEmptyArrayRatherThanNull` | ReturnEmptyCollectionRatherThanNull |
| `ReturnEmptyCollectionRatherThanNull` | ReturnEmptyCollectionRatherThanNull |
| `ShortInstantiation` | PrimitiveWrapperInstantiation |
| `SimplifyBooleanAssertion` | SimplifiableTestAssertion |
| `SimplifyStartsWith` | XPath SimplifyStartsWith |
| `StdCyclomaticComplexity` | CyclomaticComplexity |
| `SuspiciousConstantFieldName` | FieldNamingConventions |
| `UnnecessaryLocalRule` | UnnecessaryLocalBeforeReturn |
| `UnnecessaryWrapperObjectCreation` | PrimitiveWrapperInstantiation |
| `UnsynchronizedStaticDateFormatter` | UnsynchronizedStaticFormatter |
| `UnusedImports` | UnnecessaryImport |
| `UseAssertEqualsInsteadOfAssertTrue` | SimplifiableTestAssertion |
| `UseAssertNullInsteadOfAssertTrue` | SimplifiableTestAssertion |
| `UseAssertSameInsteadOfAssertTrue` | SimplifiableTestAssertion |
| `UseAssertTrueInsteadOfAssertEquals` | SimplifiableTestAssertion |
| `UseStringIsEmptyRule` | XPath UseStringIsEmptyRule |
| `VariableNamingConventions` | FieldNamingConventions,LocalVariableNamingConventions,FormalParameterNamingConventions |
| `WhileLoopsMustUseBraces` | ControlStatementBraces |
