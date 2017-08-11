# NYPL-Bibs-Items-S3-Harvester
An app to retrieve all the bibs and items that are in the bib and item json files stored in S3 and send them to kinesis stream in avro encoded format.

# To run the app locally
 * clone the repo
 * set the following env variables:

```
export awsAccessKey=your_access_key
export awsBucketName=bucket_with_bibs_items_json_files
export awsRegion=us-east-1
export awsSecretKey=your_secret_key
export bibSchemaAPI=https://api.nypltech.org/api/v0.1/current-schemas/BibPostRequest
export bibsOrItems=items|bibs
export bibsS3JsonFile=bibs.ndjson
export bibStream=kinesis_bibs_stream
export itemSchemaAPI=https://api.nypltech.org/api/v0.1/current-schemas/ItemPostRequest
export itemsS3JsonFile=items.ndjson
export itemStream=kinesis_items_stream

```
  * Run as a spring boot app from IDE otherwise just do mvn clean package for the jar file and run it using                    java -jar NYPL-*.jar
  
 # How it works
  Only bibs or items can be processed at one time. Instead of loading the entire file from S3 into the machine's memory, we are using the splitter to split based on new line token (every bib and item in the file is separated by a new line) and streaming it. Then we are using camel's parallelProcessing with default settings to process data faster.
  
 # Limitations:
 Camel AWS-S3 component has an option to deleteAfterRead. If this is set to true, it will delete the file from the S3 bucket after processing it. We don't want to delete our bibs.json and items.json files as we may need to read from those files in the future, hence setting deleteAfterRead to false results into another problem, where it will keep reading that file again even after it read it once. To get around this issue, in the camel route we are using an IdempotentConsumer and filtering for duplicate files (bibs.json/items.json) that come in and stop the process, so that we only process the files once. 
 
 # Links
 http://camel.apache.org/aws-s3.html
 http://camel.apache.org/splitter.html
 http://people.apache.org/~dkulp/camel/idempotent-consumer.html
 
