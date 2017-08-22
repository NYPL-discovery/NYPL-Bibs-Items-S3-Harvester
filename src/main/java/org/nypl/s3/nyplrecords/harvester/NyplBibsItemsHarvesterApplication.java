package org.nypl.s3.nyplrecords.harvester;

import java.nio.charset.Charset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NyplBibsItemsHarvesterApplication {

  public static void main(String[] args) {
    System.out.println("---------------------");
    System.out.println(Charset.defaultCharset());
    System.out.println("---------------------");
    SpringApplication.run(NyplBibsItemsHarvesterApplication.class, args);
  }
}
