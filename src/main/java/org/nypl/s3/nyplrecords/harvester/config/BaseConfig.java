package org.nypl.s3.nyplrecords.harvester.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

@Configuration
public class BaseConfig {

  private static Logger logger = LoggerFactory.getLogger(BaseConfig.class);

  @Bean
  public AmazonS3 getAmazonS3Client() {
    AmazonS3 amazonClient = new AmazonS3Client();
    return amazonClient;
  }

  @Bean
  public AmazonKinesisClient getAmazonKinesisClient() {
    AmazonKinesisClient amazonKinesisClient = new AmazonKinesisClient();

    logger.info("Configured Kinesis Client");

    return amazonKinesisClient;
  }

  @Bean
  public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(60000);
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(100);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

}
