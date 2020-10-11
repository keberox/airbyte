package io.airbyte.workers.protocols;

import io.airbyte.commons.functional.CheckedConsumer;
import io.airbyte.config.StandardTargetConfig;
import java.nio.file.Path;

public interface Destination<T> extends CheckedConsumer<T, Exception>, AutoCloseable {

  void start(StandardTargetConfig targetConfig, Path jobRoot) throws Exception;

  @Override
  void accept(T message) throws Exception;

  void notifyEndOfStream() throws Exception;

  @Override
  void close() throws Exception;
}
