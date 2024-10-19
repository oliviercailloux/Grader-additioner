package io.github.oliviercailloux.javagrade.graders;

import static com.google.common.base.Verify.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import io.github.oliviercailloux.contained.AdditionerClient;
import io.github.oliviercailloux.containing.MarksReceiverImpl;
import io.github.oliviercailloux.containing.StudentClassSender;
import io.github.oliviercailloux.git.github.model.GitHubUsername;
import io.github.oliviercailloux.git.github.model.RepositoryCoordinates;
import io.github.oliviercailloux.git.github.model.RepositoryCoordinatesWithPrefix;
import io.github.oliviercailloux.grade.BatchGitHistoryGrader;
import io.github.oliviercailloux.grade.Criterion;
import io.github.oliviercailloux.grade.GitFileSystemWithHistoryFetcher;
import io.github.oliviercailloux.grade.GitFileSystemWithHistoryFetcherFromMap;
import io.github.oliviercailloux.grade.GitFsGraderUsingLast;
import io.github.oliviercailloux.grade.GradeAggregator;
import io.github.oliviercailloux.grade.MarksTree;
import io.github.oliviercailloux.grade.MavenCodeHelper;
import io.github.oliviercailloux.grade.MavenCodeHelper.WarningsBehavior;
import io.github.oliviercailloux.grade.PathGrader;
import io.github.oliviercailloux.grade.format.CsvGrades;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.javagrade.bytecode.Compiler.CompilationResultExt;
import io.github.oliviercailloux.jsand.host.Containerizer;
import io.github.oliviercailloux.jsand.host.ExecutedContainer;
import io.github.oliviercailloux.jsand.host.JavaSourcer;
import io.github.oliviercailloux.jsand.host.Registerer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Additioner implements PathGrader<RuntimeException> {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Additioner.class);

  public static final ZonedDateTime DEADLINE_ORIGINAL =
      LocalDateTime.parse("2024-04-10T19:00:00").atZone(ZoneId.of("Europe/Paris"));
  public static final double USER_WEIGHT = 0d;

  public final MavenCodeHelper<RuntimeException> h =
      MavenCodeHelper.penal(WarningsBehavior.DO_NOT_PENALIZE);

  public static void main(String[] args) throws Exception {
    final GitFileSystemWithHistoryFetcher fetcher =
        GitFileSystemWithHistoryFetcherFromMap
            .fromMap(ImmutableMap.of(GitHubUsername.given("oliviercailloux-org"),
                RepositoryCoordinatesWithPrefix.from("oliviercailloux-org", "add",
                    "oliviercailloux"),
                GitHubUsername.given("oliviercailloux"),
                RepositoryCoordinates.from("oliviercailloux", "superadd")), true);
    final BatchGitHistoryGrader<RuntimeException> batchGrader =
        BatchGitHistoryGrader.given(() -> fetcher);
    batchGrader.setIdentityFunction(CsvGrades.STUDENT_USERNAME_FUNCTION);

    batchGrader.getAndWriteGradesExp(DEADLINE_ORIGINAL, Duration.ofMinutes(30),
    GitFsGraderUsingLast.using(new Additioner()), USER_WEIGHT, Path.of("grades add"),
    "add" + Instant.now().atZone(DEADLINE_ORIGINAL.getZone()));
    LOGGER.info("Done original, closed.");
  }

  private static final Criterion ADD = Criterion.given("pos");
  private static final Criterion ADD_NEG = Criterion.given("neg");

  public static void mainShort(String[] args) throws Exception {
    Registry registryJ1 = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    HelloImpl hello = new HelloImpl();
    Hello stub = (Hello) UnicastRemoteObject.exportObject(hello, 0);
    registryJ1.rebind("Hello", stub);

    Path target = Path.of("/tmp/J2/");
    if (Files.exists(target)) {
      MoreFiles.deleteRecursively(target);
    }
    Files.createDirectories(target);
    PathUtils.copyRecursively(Path.of("src/"), target.resolve("src/"));
    Path targetPom = target.resolve("pom.xml");
    Files.copy(Path.of("pom.xml"), targetPom);
    Files.copy(Path.of("settings.xml"), target.resolve("settings.xml"));

    ProcessBuilder builder = new ProcessBuilder();
    builder.command("mvn", "-Pclient", "compile", "org.codehaus.mojo:exec-maven-plugin:3.3.0:exec");
    builder.directory(target.toFile());
    builder.redirectOutput(Redirect.INHERIT);
    builder.redirectError(Redirect.INHERIT);
    Process process = builder.start();
    process.getOutputStream().close();
    // String stdOut;
    // try (BufferedReader stdR = process.inputReader(StandardCharsets.UTF_8)) {
    //   StringWriter writer = new StringWriter();
    //   stdR.transferTo(writer);
    //   stdOut = writer.toString();
    // }
    // String stdErr;
    // try (BufferedReader stdR = process.errorReader(StandardCharsets.UTF_8)) {
    //   StringWriter writer = new StringWriter();
    //   stdR.transferTo(writer);
    //   stdErr = writer.toString();
    // }
    // LOGGER.info("Output: {}", stdOut);
    // LOGGER.info("Error: {}", stdErr);
    // int exitCode = process.waitFor();
    // verify(exitCode == 0, "Exit code: " + exitCode);

    LOGGER.info("Querying.");
    hello.latch().await();
    Thread.sleep(5000);
    LOGGER.info("Queried.");

    Registry registryJ2 = LocateRegistry.getRegistry(Registry.REGISTRY_PORT + 1);
    RemoteTest rem = (RemoteTest) registryJ2.lookup("RemoteTestJ2");
    LOGGER.info("Engine: {}", rem);
    int tested = rem.test(0, 1);
    LOGGER.info("Tested: {}", tested);
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));
    Thread.sleep(1000);
    LOGGER.info("Tested: {}", rem.test(0, 1));

    // LOGGER.info("Registry: {}", ImmutableList.copyOf(registryJ1.list()));
  }

  private static void compile(Path target) throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder();
    // builder.command("mvn", "-Pclient", "compile",
    // "org.codehaus.mojo:exec-maven-plugin:3.3.0:java");
    builder.command("mvn", "-Pclient", "compile");
    builder.directory(target.toFile());
    Process process = builder.start();
    int exitCode = process.waitFor();
    process.getOutputStream().close();
    String stdOut;
    try (BufferedReader stdR = process.inputReader(StandardCharsets.UTF_8)) {
      StringWriter writer = new StringWriter();
      stdR.transferTo(writer);
      stdOut = writer.toString();
    }
    String stdErr;
    try (BufferedReader stdR = process.errorReader(StandardCharsets.UTF_8)) {
      StringWriter writer = new StringWriter();
      stdR.transferTo(writer);
      stdErr = writer.toString();
    }
    LOGGER.info("Output: {}", stdOut);
    LOGGER.info("Error: {}", stdErr);
    verify(exitCode == 0, "Exit code: " + exitCode);
  }

  @Override
  public MarksTree grade(Path path) {
    try {
      return gradeE(path);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public MarksTree gradeE(Path path) throws Exception {
    CompilationResultExt result = h.compile(path);
    Path containedDir = Files.createTempDirectory("contained");
        JavaSourcer sourcer = JavaSourcer.targetDir(containedDir);
    PathUtils.copyRecursively(Path.of(""), containedDir);
    sourcer.copyLogbackConf();

    Containerizer containerizer =
        Containerizer.usingPaths(containedDir, Path.of("/home/olivier/.m2/repository/"));
    containerizer.createNetworksIfNotExist();

    containerizer.compile();

    Registerer registerer = Registerer.create();
    registerer.setHostIp(containerizer.hostIp());
    registerer.ensureRegistry();
    registerer.registerLogger();
    registerer.registerClassSender(StudentClassSender.create(result.compiledDir));
    MarksReceiverImpl marksReceiver = MarksReceiverImpl.create();
    registerer.register("MarksReceiver", marksReceiver);
    
    ExecutedContainer ran = containerizer.run(AdditionerClient.class.getName());
    assertTrue(ran.err().length() < 10, ran.err());
    assertTrue(ran.out().contains("BUILD SUCCESS"));
    assertEquals(0, ran.exitCode());

    containerizer.removeContainersIfExist();

    return marksReceiver.marks();
  }

  @Override
  public GradeAggregator getAggregator() {
    final ImmutableMap.Builder<Criterion, Double> innerBuilder = ImmutableMap.builder();
    innerBuilder.put(ADD, 3d);
    innerBuilder.put(ADD_NEG, 2d);
    return GradeAggregator.staticAggregator(innerBuilder.build(), ImmutableMap.of());
  }
}
