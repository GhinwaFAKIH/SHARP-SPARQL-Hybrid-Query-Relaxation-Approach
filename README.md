**SHARP: A Hybrid Approach for SPARQL Query Relaxation**

This repository implements a hybrid SPARQL query relaxation framework, which relies on precomputed statistics and mapping functions to support entity- and ontology-based relaxation. It is build upon OMBS[1] (an optimized ontology-based relaxation strategy). 

**Step 1: Compute Dataset Statistics and mapping function matrices**


Before running any query relaxation tasks, global dataset statistics must be computed. 


To compute statistics, run the Statistics java file that extracts and stores statistics.

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
























[1]: Fokou, G., Jean, S., Hadjali, A. and Baron, M., 2016. RDF query relaxation strategies based on failure causes. In The Semantic Web. Latest Advances and New Domains: 13th International Conference, ESWC 2016, Heraklion, Crete, Greece, May 29--June 2, 2016, Proceedings 13 (pp. 439-454). Springer International Publishing.

