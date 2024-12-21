package io.github.oliviercailloux.contained;

import io.github.oliviercailloux.Grader;
import io.github.oliviercailloux.congrad.MarksReceiver;
import io.github.oliviercailloux.congrad.contained.InstanciatorOnRemote;
import io.github.oliviercailloux.grade.MarksTree;
import io.github.oliviercailloux.javagrade.JUnitHelper;
import io.github.oliviercailloux.jsand.common.ClassSenderService;
import io.github.oliviercailloux.jsand.containerized.ClassFetcher;
import io.github.oliviercailloux.jsand.containerized.HostRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionerClient {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(AdditionerClient.class);
  
  public static void main(String[] args) throws Exception {
    HostRegistry hostRegistry = HostRegistry.access();
    MarksReceiver r = (MarksReceiver) hostRegistry.rmiRegistry().lookup("MarksReceiver");
    ClassSenderService sender = hostRegistry.classSenderService();
    LOGGER.info("Preparing mark.");
    String packageName = Grader.class.getPackageName();
    MarksTree grade = JUnitHelper.grade(packageName, InstanciatorOnRemote.create(ClassFetcher.create(sender)));
    r.receiveMarks(grade);
    LOGGER.info("Mark sent.");
  }
}
