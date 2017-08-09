package org.nypl.s3.nyplrecords.harvester.config;

public class EnvironmentConfig {

  public static final String BUCKET_NAME = System.getenv("awsBucketName");
  public static final String AMAZON_ACCESS_KEY = System.getenv("awsAccessKey");
  public static final String AMAZON_SECRET_KEY = System.getenv("awsSecretKey");
  public static final String AMAZON_REGION = System.getenv("awsRegion");
  public static final Boolean DOWNLOAD_FILES_FROM_S3 =
      Boolean.parseBoolean(System.getenv("downloadFileAndThenRun"));
  public static final String BIBS_S3_JSON_FILE = System.getenv("bibsS3JsonFile");
  public static final String ITEMS_S3_JSON_FILE = System.getenv("itemsS3JsonFile");
  public static final String BIB_SCHEMA_API = System.getenv("bibSchemaAPI");
  public static final String ITEM_SCHEMA_API = System.getenv("itemSchemaAPI");
  public static final String KINESIS_BIB_STREAM = System.getenv("bibStream");
  public static final String KINESIS_ITEM_STREAM = System.getenv("itemStream");
  public static final String BIB_OR_ITEM = System.getenv("bibsOrItems");

}
