package io.github.oliviercailloux.contained;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.VerifyException;
import io.github.oliviercailloux.contained.SimpleInstanciator;
import io.github.oliviercailloux.exercices.additioner.MyAdditioner;
import io.github.oliviercailloux.javagrade.JUnitHelper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdditionerTests {
  public static SimpleInstanciator i;
  public MyAdditioner a;

  @BeforeAll
  public static void setUp() {
    i = JUnitHelper.staticInstanciator;
  }

  @BeforeEach
  public void setUpEach() throws Exception {
    a = i.newInstance("io.github.oliviercailloux.exercices.additioner.AddImpl", MyAdditioner.class);
  }

  @Test
  public void pos() throws Exception {
    assertEquals(5, a.add(3, 2), "Trying to add 3 and 2.");
  }

  @Test
  public void neg() throws Exception {
    assertEquals(-2, a.add(-4, 2), "Trying to add -4 and 2.");
  }
}
