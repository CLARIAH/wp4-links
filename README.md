# wp4-links

Code for linking all Dutch civil registries. It's purpose, use case, technical features and limitations are described in the <a href="https://github.com/CLARIAH/wp4-links/blob/master/documentation.md">documentation</a>.


## How does it work?
1. Convert all CSV files to RDF according to our simple [Civil Registries schema](assets/LINKS-schema.png), based on Schema.org and BIO vocabularies. You can browse the [RDF file](assets/LINKS-schema.ttl) in any triple store (e.g. Druid) or Ontology editor (e.g. Protégé).
  - We use COW for converting the CSV files to RDF, but any other method would also work as long as the data in RDF are modelled according to the  [Civil Registries schema](assets/LINKS-schema.png).

2. Merge all resulting RDF files into one larger file:
  - ```cat birth-registrations.nq birth-persons.nq marriage-registrations.nq marriage-persons.nq > all-civil-registries.nq```

3. Install JAVA Runtime Environment (JRE), which is almost installed in every computer these days
  - Free [download](https://www.oracle.com/java/technologies/javase-jre8-downloads.html) from Oracle.

4. Convert the large RDF file to HDT (this process might require a lot of memory usage depending on the dataset size).
  - ```nohup rdf2hdt all-civil-registries.nq all-civil-registries.hdt -i -p &``` (-i for also creating the index to speed-up the reading)
  - Or using the function `convertToHDT` in the linking tool

5. Start linking from terminal :)
  - ```java -jar links.jar```

## Example for linking all marriage certificates with a maximum Levenshtein of 1
```java -jar links.jar --function Between_M_M --maxLev 1 --format CSV --inputData all-civil-registries.hdt --outputDir .```
