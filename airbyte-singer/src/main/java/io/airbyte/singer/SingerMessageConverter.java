package io.airbyte.singer;

import io.airbyte.protocol.models.AirbyteMessage;
import io.airbyte.protocol.models.AirbyteRecordMessage;
import io.airbyte.protocol.models.AirbyteStateMessage;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

public class SingerMessageConverter {

  public static Optional<AirbyteMessage> toAirbyteMessage(final SingerMessage message) {
    final AirbyteMessage airbyteMessage;
    switch (message.getType()) {
      case RECORD -> {
        airbyteMessage = new AirbyteMessage();
        airbyteMessage.setType(AirbyteMessage.Type.RECORD);
        airbyteMessage.setRecord(new AirbyteRecordMessage()
            .withStream(message.getStream())
            .withData(message.getRecord())
            .withEmittedAt(Instant.parse(message.getTimeExtracted()).getEpochSecond()));
      }
      case STATE -> {
        airbyteMessage = new AirbyteMessage();
        airbyteMessage.setType(AirbyteMessage.Type.STATE);
        airbyteMessage.setState(new AirbyteStateMessage().withData(message.getValue()));
      }
      default -> {
        airbyteMessage = null;
      }
    }
    return Optional.ofNullable(airbyteMessage);
  }

  public static Optional<SingerMessage> toSingerMessage(final AirbyteMessage message) {
    final SingerMessage singerMessage;
    switch (message.getType()) {
      case RECORD -> {
        singerMessage = new SingerMessage();
        singerMessage.setType(SingerMessage.Type.RECORD);
        singerMessage.setRecord(message.getRecord().getData());
        singerMessage.setTimeExtracted(
             ZonedDateTime.ofInstant(
                 Instant.ofEpochSecond(message.getRecord().getEmittedAt()),
                 ZoneOffset.UTC).toString());
      }
      case STATE -> {
        singerMessage = new SingerMessage();
        singerMessage.setType(SingerMessage.Type.STATE);
        singerMessage.setValue(message.getState().getData());
      }
      default -> {
        singerMessage = null;
      }
    }
    return Optional.ofNullable(singerMessage);
  }

}
