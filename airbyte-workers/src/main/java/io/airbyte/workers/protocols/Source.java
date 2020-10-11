package io.airbyte.workers.protocols;

import io.airbyte.config.StandardTapConfig;
import io.airbyte.singer.SingerMessage;
import java.nio.file.Path;
import java.util.Optional;

public interface Source<T> extends AutoCloseable {

  void start(StandardTapConfig input, Path jobRoot) throws Exception;

  boolean isFinished();

  Optional<T> attemptRead();

  @Override
  void close() throws Exception;
}
