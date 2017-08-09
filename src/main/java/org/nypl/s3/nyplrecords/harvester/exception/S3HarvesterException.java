package org.nypl.s3.nyplrecords.harvester.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3HarvesterException extends Exception {

  private static final long serialVersionUID = 1L;

  private static Logger logger = LoggerFactory.getLogger(S3HarvesterException.class);

  public S3HarvesterException(String message) {
    logger.error("S3HarvesterException occurred - " + message);
  }

}
