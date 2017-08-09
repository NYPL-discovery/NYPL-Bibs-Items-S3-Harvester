package org.nypl.s3.nyplrecords.harvester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NyplBibsItemsHarvesterApplication {

  public static void main(String[] args) {
    SpringApplication.run(NyplBibsItemsHarvesterApplication.class, args);
  }
}
