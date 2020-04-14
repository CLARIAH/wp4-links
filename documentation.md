# LINKS linkage tool

## Purpose
- to improve (and replace) the current [LINKS](https://iisg.amsterdam/en/hsn/projects/links) software. Points of improvement are:
    - extremely fast and scalable matching procedure using Levenshtein automata; 
    - written in a generally used, all purpose language (Java);
    - open software

## Use case
Historians use archival records to describe persons lives. Each record (e.g. a marriage record) just describes a point in time. Hence historians try to link multiple records on the same person (e.g.) birth, marriage, death to describe a life course. This tool focuses on ‘just' the linkage of civil records. By doing so, pedigrees of humans can be created over multiple generations for research on social inequality, especially in the part of health sciences where the focus is on gene-social contact interactions.

## User profile
The software is designed for the so called ‘digital’ historian: e.g. someone with basic command line skills or similar (e.g. Python, R).

## What it is not 
In its current version, it cannot be used to match entities from just any source. The current tool is solely focused on the linkage of civil records, relying on the sanguineous relations on the civil record, modelled according to our Civil Registers schema. 


# Technical specifications
## Installation requirements
- Only the JAVA Runtime Environment (JRE), which is almost installed in every computer these days
Free download: https://www.oracle.com/java/technologies/javase-jre8-downloads.html

## Input requirements
- Only one RDF dataset, with the data being modelled according to our simple Civil Registries schema, based on Schema.org and BIO vocabularies as advocated in the Golden Agents project.
For efficient querying (less memory usage, fast search), the matching tool requires the dataset to be compressed and given as an HDT file with its index [1]. The tool allows the conversion of an RDF file (any serialisation) to HDT using the --function convertToHDT.
[1] [What is HDT?](http://www.rdfhdt.org/what-is-hdt/)

## Output format
Two possible output formats to represent the detected links:
- N-QUADS file (default if no output format is specified by the user)
- CSV file  
It can be specified in the input of the tool using --format RDF or --format CSV

## Main dependencies (installed through Maven)
- Levenshtein automata (MIT License): https://github.com/universal-automata/liblevenshtein-java
- RDF-HDT (LGPL License): https://github.com/rdfhdt/hdt-java

## Distribution
- Open source code available on the CLARIAH Github repo: https://github.com/CLARIAH/wp4-links
- To Do: Docker Container

# Functionality
The current version has six functionalities, specified by the user using --function [functionalityName]:
- `convertToHDT`: convert an RDF file to an HDT file
- `showDatasetStats`: show in console some general stats about the input HDT dataset 
- `linkNewbornToPartner`: link newborns in Birth Certificates to brides/grooms in Marriage Certificates
- `linkPartnerToPartner`: link parents of brides/grooms in Marriage Certificates to brides and grooms in Marriage Certificates
- `linkSiblings`: link parents of newborns in Birth Certificates to parents of newborns in Birth Certificates (for detecting siblings)  
- `linkMarriageParentsToMarriageParents`: link parents of brides/grooms in Marriage Certificates to parents of brides/grooms in Marriage Certificates (for detecting siblings)

## All parameters that can be provided as input for the linkage tool:
- `--function` (required): One of the 6 following functionalities: [showDatasetStats, convertToHDT, linkNewbornToPartner, linkPartnerToPartner, linkSiblings, linkMarriageParentsToMarriageParents]
- `--inputData` (required for all functions): Path to the HDT dataset
- `--outputDir` (required for all functions, except for "showDatasetStats"): Path to the directory for saving the indices and the links
- `--maxLev` (required for all functions, except "showDatasetStats" and "convertToHDT"): Integer between 0 and 5, specifying the maximum Levenshtein distance allowed
- `--format` (optional for all functions, except "showDatasetStats" and "convertToHDT"): One of the two Strings: RDF (default) or CSV, specifying the desired format to save the links between certificates 
- `--debug` (optional for all functions): One of the two Strings: error (default, showing only errors in console that occurred in the matching), all (showing every warning in console)


## Example for linking marriage certificates in Zeeland dataset:
```java -jar links.jar --function linkPartnerToPartner --inputData dataset-zeeland/certificates-zeeland.hdt --outputDir . --maxLev 1 --format CSV```


# Performance
- Matching ~700K newborns to ~190K brides/grooms based on the names similarity between the three following pairs of individuals: 
(newborn and bride/groom), (mother of newborn and mother of bride/groom) and (father of newborn and father of bride/groom) takes:
    - 5 minutes for maximum Levenshtein distance of 1 per name (up-to 3 for the 3 pairs) on a MacBook Pro with 16GB memory 
    - 18 minutes for maximum Levenshtein distance of 2 per name (up-to 6 for the 3 pairs)
    - 74 minutes for maximum Levenshtein distance of 3 per name (up-to 9 for the 3 pairs)
- ~20 hours for matching all Dutch marriage certificates from 1812 to 1919 (~8.8M parents matched to ~4.4M bride/grooms)  

# Possible direct extensions
It would be possible to add more general matching functionalities that are not dependent on the Civil Registries schema.
One possible way would be to provide a JSON Schema as an additional input to any given dataset, specifying the (i) Classes that the user wish to match their instances (e.g. sourceClass: iisg:Newborn ; targetClass: iisg:Groom), and the (ii) Properties that should be considered in the matching (e.g. schema:givenName; schema:familyName). 

Subsequently, the fast matching algorithm could be used for many other linkage purposes (in Digital Humanities), e.g. places, occupations and products.




