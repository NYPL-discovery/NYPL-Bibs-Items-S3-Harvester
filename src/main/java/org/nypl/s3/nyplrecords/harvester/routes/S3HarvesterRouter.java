package org.nypl.s3.nyplrecords.harvester.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.builder.RouteBuilder;
import org.nypl.s3.nyplrecords.harvester.config.BaseConfig;
import org.nypl.s3.nyplrecords.harvester.config.EnvironmentConfig;
import org.nypl.s3.nyplrecords.harvester.exception.S3HarvesterException;
import org.nypl.s3.nyplrecords.harvester.schema.BibAvroSchema;
import org.nypl.s3.nyplrecords.harvester.schema.ItemAvroSchema;
import org.nypl.s3.nyplrecords.harvester.streams.KinesisProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;

@Component
public class S3HarvesterRouter extends RouteBuilder {

  @Autowired
  private BibAvroSchema bibAvroSchema;

  @Autowired
  private ItemAvroSchema itemAvroSchema;

  @Autowired
  private ProducerTemplate producerTemplate;

  @Autowired
  private RetryTemplate retryTemplate;

  @Autowired
  private BaseConfig baseConfig;
  
  private static final String NYPL_SOURCE = "nyplSource";

  private static final String NYPL_TYPE = "nyplType";

  private static final String SIERRA_NYPL_SOURCE = "sierra-nypl";

  private static final String BIB = "bib";

  private static final String ITEM = "item";

  private static final String ORDERS = "orders";

  public static final String PARTITION_KEY = "CamelAwsKinesisPartitionKey";
  public static final String SEQUENCE_NUMBER = "CamelAwsKinesisSequenceNumber";

  @Override
  public void configure() throws Exception {

    String bibOrItem = EnvironmentConfig.BIB_OR_ITEM;
    if (bibOrItem.endsWith("s")) {
      bibOrItem = bibOrItem.substring(0, bibOrItem.indexOf("s"));
    }
    bibOrItem = bibOrItem.toLowerCase();

    if (bibOrItem.equals(BIB)) {
      from("aws-s3://" + EnvironmentConfig.BUCKET_NAME + "?fileName="
          + EnvironmentConfig.BIBS_S3_JSON_FILE
          + "&amazonS3Client=#getAmazonS3Client&deleteAfterRead=false&maxMessagesPerPoll=10")
              .idempotentConsumer(header("CamelAwsS3Key"),
                  MemoryIdempotentRepository.memoryIdempotentRepository()).skipDuplicate(false)
              .filter(property(Exchange.DUPLICATE_MESSAGE).isEqualTo(true))
                .log("Received a duplicate message")
                .process(new Processor() {
                  
                  @Override
                  public void process(Exchange exchange) throws Exception {
                    System.exit(0);
                  }
                }).end() 
              .split(body().tokenize("\n")).streaming().parallelProcessing().stopOnException()
              .process(new Processor() {

                @Override
                public void process(Exchange exchange) throws Exception {
                  if (bibAvroSchema.getSchema() == null) {
                    bibAvroSchema.setSchema(retryTemplate, producerTemplate);
                  }
                  String nyplRecord = exchange.getIn().getBody(String.class);
                  Map<String, Object> nyplRecordKeyVals =
                      new ObjectMapper().readValue(nyplRecord, Map.class);
                  nyplRecordKeyVals.put(NYPL_SOURCE, SIERRA_NYPL_SOURCE);
                  nyplRecordKeyVals.put(NYPL_TYPE, BIB);
                  if (nyplRecordKeyVals.containsKey(ORDERS))
                    nyplRecordKeyVals.remove(ORDERS);
                  exchange.getIn().setHeader(PARTITION_KEY, System.currentTimeMillis());
                  exchange.getIn().setHeader(SEQUENCE_NUMBER, System.currentTimeMillis());
                  exchange.getIn().setBody(nyplRecordKeyVals);
                }
              }).process(new Processor() {

                @Override
                public void process(Exchange exchange) throws Exception {
                  Schema schema =
                      new Schema.Parser().setValidate(true).parse(bibAvroSchema.getSchema());
                  AvroSchema avroSchema = new AvroSchema(schema);
                  AvroMapper avroMapper = new AvroMapper();
                  byte[] avroBib = avroMapper.writer(avroSchema)
                      .writeValueAsBytes(exchange.getIn().getBody(Map.class));
                  System.out.println(exchange.getIn().getBody(Map.class));
                  List<byte[]> avroBibList = new ArrayList<>();
                  avroBibList.add(avroBib);
                  exchange.getIn().setBody(avroBibList);
                }
              }).process(new KinesisProcessor(baseConfig, EnvironmentConfig.KINESIS_BIB_STREAM));
    } else {
      from("aws-s3://" + EnvironmentConfig.BUCKET_NAME + "?fileName="
          + EnvironmentConfig.ITEMS_S3_JSON_FILE
          + "&amazonS3Client=#getAmazonS3Client&deleteAfterRead=false&maxMessagesPerPoll=10")
              .idempotentConsumer(header("CamelAwsS3Key"),
                  MemoryIdempotentRepository.memoryIdempotentRepository()).skipDuplicate(false)
              .filter(property(Exchange.DUPLICATE_MESSAGE).isEqualTo(true))
              .log("Received a duplicate message")
              .process(new Processor() {
                
                @Override
                public void process(Exchange exchange) throws Exception {
                  System.exit(0);
                }
              }).end()
              .split(body().tokenize("\n")).streaming().parallelProcessing().stopOnException()
              .process(new Processor() {

                @Override
                public void process(Exchange exchange) throws Exception {
                  if (itemAvroSchema.getSchema() == null) {
                    itemAvroSchema.setSchema(retryTemplate, producerTemplate);
                  }
                  String nyplRecord = exchange.getIn().getBody(String.class);
                  Map<String, Object> nyplRecordKeyVals =
                      new ObjectMapper().readValue(nyplRecord, Map.class);
                  nyplRecordKeyVals.put(NYPL_SOURCE, SIERRA_NYPL_SOURCE);
                  nyplRecordKeyVals.put(NYPL_TYPE, ITEM);
                  if (nyplRecordKeyVals.containsKey(ORDERS))
                    nyplRecordKeyVals.remove(ORDERS);
                  exchange.getIn().setHeader(PARTITION_KEY, System.currentTimeMillis());
                  exchange.getIn().setHeader(SEQUENCE_NUMBER, System.currentTimeMillis());
                  exchange.getIn().setBody(nyplRecordKeyVals);
                }
              }).process(new Processor() {

                @Override
                public void process(Exchange exchange)
                    throws S3HarvesterException, JsonProcessingException {
                  Schema schema =
                      new Schema.Parser().setValidate(true).parse(itemAvroSchema.getSchema());
                  AvroSchema avroSchema = new AvroSchema(schema);
                  AvroMapper avroMapper = new AvroMapper();
                  byte[] avroItem = avroMapper.writer(avroSchema)
                      .writeValueAsBytes(exchange.getIn().getBody(Map.class));
                  System.out.println(exchange.getIn().getBody(Map.class));
                  List<byte[]> avroItemList = new ArrayList<>();
                  avroItemList.add(avroItem);
                  exchange.getIn().setBody(avroItemList);
                }
              }).process(new KinesisProcessor(baseConfig, EnvironmentConfig.KINESIS_ITEM_STREAM));
    }

  }

}
