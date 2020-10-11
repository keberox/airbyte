package io.airbyte.workers.protocols;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.function.Consumer;

public interface MessageTracker<T> extends Consumer<T> {

  @Override
  public void accept(T message);

  long getRecordCount();

  Optional<JsonNode> getOutputState();

}
