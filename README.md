# 🚀 SHARP: A Hybrid Approach for SPARQL Query Relaxation
SHARP is a hybrid SPARQL query relaxation system that integrates ontology-based and entity-based strategies to improve query results when exact matches are unavailable. 
It handles relaxations on classes, properties, entities, and literals using an information-content-based ranking system. SHARP addresses the combinatorial challenge of multiple relaxations and provides fair ranking across different relaxation types.
This repository implements a hybrid SPARQL query relaxation framework, which relies on precomputed statistics and mapping functions to support entity- and ontology-based relaxation. It is build upon OMBS[1] (an optimized ontology-based relaxation strategy). 

# 🎯 Project Aim:

✅ Retrieve top-k relevant results when SPARQL queries return no or few matches.

✅ Combine ontology-based (class/property) and entity-based (instance/literal) relaxation strategies into a hybrid model.

✅ Rank relaxed query results using a unified, information-content-based similarity measure.

✅ Provide an extended benchmark (based on LUBM4OBDA) with new queries targeting different relaxation types.

✅ Enable evaluation of relaxation models using human-judgment-based relevance assessments.


# 🧱 Project Structure
**`src/`**: Houses all the core classes of the project:


• start/:  
  Entry point of the application. Initializes configurations, loads data, triggers the relaxation and ranking pipeline, and prints top-k results.

• Similarity/:  
  Contains modules for computing similarity between classes, properties, entities, and literals.

• RDFStarRelaxer/:  
  Implements the core logic specific to RDF-Star query relaxation, extending standard SPARQL relaxation to support reification.

• utils/:  
  Provides utility functions and helper classes for common tasks.



**`CONNOR/`**:Contains the full source code for the entity-based relaxation method used in the evaluation.
Refer to the original project [here](https://gitlab.inria.fr/hayats/CONNOR).



**`Benchmark/`**: Contains the seven new queries, each written in natural language and accompanied by its context. Each query is also accompanied by a set of relaxed queries ranked based on a user survey.


**`BENCH-Answers/`**: Contains the answers to the queries, ranked according to the order of relaxed queries in the benchmark.


**`OMBS_results/`**: Contains the results of the ontology-based relaxation model (OMBS).


**`SHARP_results/`**: Contains the results of our hybrid relaxation model SHARP.



# 🧱 Running Steps
**Step 1: Compute Dataset Statistics and mapping function matrices**

Before running any query relaxation tasks, global dataset statistics must be computed. 

To compute statistics, run the Statistics java file that extracts and stores statistics (src/Statistics.java).

Embeddings were computed using RDFStar2vec (Source code could be found here: https://github.com/aistairc/RDF-star2Vec).


**Step 2: Prepare Dataset**
- Load the ontology and RDF data (e.g., LUBM) into Fuseki.
 The ontology file used should be located at:
 data/univ-bench2.owl


**Step 3: Configuring parameters**
Before running the evaluation, make sure to configure the following parameters in strategy in the file src/QueryEvaluation.java:
- strategy: specifies the query relaxation strategy to use; either "OMBS". or "SHARP".
- queries: specify the query that you want to relax; e.g. "Q1".
- endpoint: URL of the SPARQL endpoint where queries will be executed and where the dataset is stored.


In file src/Similarity/TriplePatternSimilarity.java, specify path to entity and literal mapping matrices.
In file src/RDFStarRelaxer/TriplePatternRelaxer.java, specify path to entity and literal mapping matrices.
 

**Step 4: Compiling and running the relaxation code**

- Compiling: javac -cp "libraries/*" -d bin src/start/QueryEvaluation.java src/org/aksw/simba/start/*.java src/RDFStarRelaxer/*.java src/Similarity/*.java

- Running: java -cp "bin:libraries/*" start.QueryEvaluation

**Benchmark Description:**


The benchmark folder contains:

7 benchmark queries (Q1 to Q7) written in SPARQL.


For each query:

- A set of relaxed queries generated through different relaxation strategies.

- These relaxed queries are ranked manually based on human judgments collected via a user survey.



The folder BENCH-Answers contains:

- The most relevant answers for each benchmark query.

- These answers are obtained by executing the relaxed queries in the order determined by human evaluation.


## License

**Shield:**  
![License](https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey)

This work is licensed under a [Creative Commons Attribution-NonCommercial 4.0 International License](https://creativecommons.org/licenses/by-nc/4.0/).

[![CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

























[1]: Fokou, G., Jean, S., Hadjali, A. and Baron, M., 2016. RDF query relaxation strategies based on failure causes. In The Semantic Web. Latest Advances and New Domains: 13th International Conference, ESWC 2016, Heraklion, Crete, Greece, May 29--June 2, 2016, Proceedings 13 (pp. 439-454). Springer International Publishing.

