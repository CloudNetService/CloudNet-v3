package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeString implements QuestionAnswerType<String> {

  @Override
  public boolean isValidInput(@NotNull String input) {
    return true;
  }

  @Override
  public @NotNull String parse(@NotNull String input) {
    return input;
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return null;
  }

}
