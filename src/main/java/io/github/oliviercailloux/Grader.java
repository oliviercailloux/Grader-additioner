package io.github.oliviercailloux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.congrad.containing.MarksReceiverImpl;
import io.github.oliviercailloux.contained.AdditionerClient;
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
import io.github.oliviercailloux.jsand.host.ClassSender;
import io.github.oliviercailloux.jsand.host.Containerizer;
import io.github.oliviercailloux.jsand.host.ExecutedContainer;
import io.github.oliviercailloux.jsand.host.JavaSourcer;
import io.github.oliviercailloux.jsand.host.Registerer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grader implements PathGrader<RuntimeException> {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Grader.class);

  public static final ZonedDateTime DEADLINE_ORIGINAL =
      LocalDateTime.parse("2024-04-10T19:00:00").atZone(ZoneId.of("Europe/Paris"));
  public static final double USER_WEIGHT = 0d;

  public final MavenCodeHelper<RuntimeException> h =
      MavenCodeHelper.penal(WarningsBehavior.DO_NOT_PENALIZE);

  public static void main(String[] args) throws Exception {
    ImmutableMap.Builder<GitHubUsername, RepositoryCoordinates> builder = ImmutableMap.builder();
    GitHubUsername oorg = GitHubUsername.given("oliviercailloux-org");
    RepositoryCoordinates oorgAddO =
        RepositoryCoordinatesWithPrefix.from("oliviercailloux-org", "add", "oliviercailloux");
    builder.put(oorg, oorgAddO);
    GitHubUsername o = GitHubUsername.given("oliviercailloux");
    RepositoryCoordinates oSuperAdd = RepositoryCoordinates.from("oliviercailloux", "superadd");
    builder.put(o, oSuperAdd);
    ImmutableMap<GitHubUsername, RepositoryCoordinates> toBeTested = builder.build();

    final GitFileSystemWithHistoryFetcher fetcher =
        GitFileSystemWithHistoryFetcherFromMap.fromMap(toBeTested, true);
    final BatchGitHistoryGrader<RuntimeException> batchGrader =
        BatchGitHistoryGrader.given(() -> fetcher);
    batchGrader.setIdentityFunction(CsvGrades.STUDENT_USERNAME_FUNCTION);

    batchGrader.getAndWriteGradesExp(DEADLINE_ORIGINAL, Duration.ofMinutes(30),
        GitFsGraderUsingLast.using(new Grader()), USER_WEIGHT, Path.of("grades add"),
        "add" + Instant.now().atZone(DEADLINE_ORIGINAL.getZone()));
    LOGGER.info("Done original, closed.");
  }

  private static final Criterion ADD = Criterion.given("pos");
  private static final Criterion ADD_NEG = Criterion.given("neg");

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
    Files.delete(containedDir.resolve("src/main/resources/logback.xml"));
    sourcer.copyLogbackConf();

    Containerizer containerizer =
        Containerizer.usingPaths(containedDir, Path.of("/home/olivier/.m2/repository/"));
    containerizer.setProfile("client");
    containerizer.createNetworksIfNotExist();

    containerizer.compile();

    Registerer registerer = Registerer.create();
    registerer.setHostIp(containerizer.hostIp());
    registerer.ensureRegistry();
    registerer.registerLogger();
    registerer.registerClassSender(ClassSender.create(result.compiledDir));
    MarksReceiverImpl marksReceiver = MarksReceiverImpl.create();
    registerer.register("MarksReceiver", marksReceiver);

    ExecutedContainer ran = containerizer.run(AdditionerClient.class.getName());
    assertTrue(ran.err().length() < 10, ran.err());
    assertTrue(ran.out().contains("BUILD SUCCESS"));
    assertEquals(0, ran.exitCode());
    registerer.unexport();
    
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
