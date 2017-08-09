package org.nypl.s3.nyplrecords.harvester.schema;

import org.apache.camel.ProducerTemplate;
import org.nypl.s3.nyplrecords.harvester.config.EnvironmentConfig;
import org.nypl.s3.nyplrecords.harvester.exception.S3HarvesterException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class ItemAvroSchema {

  private String schema;

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public void setSchema(RetryTemplate retryTemplate, ProducerTemplate producerTemplate)
      throws S3HarvesterException {
    setSchema(new SchemaUtils().getSchema(retryTemplate, producerTemplate,
        EnvironmentConfig.ITEM_SCHEMA_API));
  }

}
