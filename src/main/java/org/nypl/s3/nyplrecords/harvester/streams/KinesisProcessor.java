package org.nypl.s3.nyplrecords.harvester.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.nypl.s3.nyplrecords.harvester.config.BaseConfig;
import org.nypl.s3.nyplrecords.harvester.config.EnvironmentConfig;
import org.nypl.s3.nyplrecords.harvester.exception.S3HarvesterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.collect.Lists;

public class KinesisProcessor implements Processor {

  private BaseConfig baseConfig;

  private String streamName;

  private static final int KINESIS_PUT_RECORDS_MAX_SIZE = 500;

  private static Logger logger = LoggerFactory.getLogger(KinesisProcessor.class);

  public KinesisProcessor(BaseConfig baseConfig, String streamName) {
    this.baseConfig = baseConfig;
    this.streamName = streamName;
  }

  @Override
  public void process(Exchange exchange) throws S3HarvesterException {
    try {
      List<byte[]> avroRecords = exchange.getIn().getBody(List.class);
      List<List<byte[]>> listOfSplitRecords =
          Lists.partition(avroRecords, KINESIS_PUT_RECORDS_MAX_SIZE);
      for (List<byte[]> splitAvroRecords : listOfSplitRecords) {
        sendToKinesis(splitAvroRecords);
      }
    } catch (Exception e) {
      logger.error("Error occurred while sending items to kinesis - ", e);
      throw new S3HarvesterException(
          "Error occurred while sending records to kinesis - " + e.getMessage());
    }
  }

  public boolean sendToKinesis(List<byte[]> avroRecords) throws S3HarvesterException {
    try {
      PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
      putRecordsRequest.setStreamName(streamName);
      List<PutRecordsRequestEntry> listPutRecordsRequestEntry = new ArrayList<>();
      for (byte[] avroItem : avroRecords) {
        PutRecordsRequestEntry putRecordsRequestEntry = new PutRecordsRequestEntry();
        putRecordsRequestEntry.setData(ByteBuffer.wrap(avroItem));
        putRecordsRequestEntry.setPartitionKey(Long.toString(System.currentTimeMillis()));
        listPutRecordsRequestEntry.add(putRecordsRequestEntry);
      }
      putRecordsRequest.setRecords(listPutRecordsRequestEntry);
      PutRecordsResult putRecordsResult =
          baseConfig.getAmazonKinesisClient().putRecords(putRecordsRequest);
      while (putRecordsResult.getFailedRecordCount() > 0) {
        final List<PutRecordsRequestEntry> failedRecordsList = new ArrayList<>();
        final List<PutRecordsResultEntry> listPutRecordsResultEntry = putRecordsResult.getRecords();
        for (int i = 0; i < listPutRecordsResultEntry.size(); i++) {
          final PutRecordsRequestEntry putRecordsRequestEntry = listPutRecordsRequestEntry.get(i);
          final PutRecordsResultEntry putRecordsResultEntry = listPutRecordsResultEntry.get(i);
          if (putRecordsResultEntry.getErrorCode() != null) {
            failedRecordsList.add(putRecordsRequestEntry);
          }
          listPutRecordsRequestEntry = failedRecordsList;
          putRecordsRequest.setRecords(listPutRecordsRequestEntry);
          putRecordsResult = baseConfig.getAmazonKinesisClient().putRecords(putRecordsRequest);
        }
      }
      return true;
    } catch (Exception e) {
      logger.error("Error occurred while sending items to kinesis - ", e);
      throw new S3HarvesterException(
          "Error occurred while sending items to kinesis - " + e.getMessage());
    }
  }

}
