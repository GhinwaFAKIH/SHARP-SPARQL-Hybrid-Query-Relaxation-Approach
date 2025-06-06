import json
import numpy as np
import csv

# Function to compute cosine similarity
def cosine_similarity(vec1, vec2):
    dot_product = np.dot(vec1, vec2)
    norm_vec1 = np.linalg.norm(vec1)
    norm_vec2 = np.linalg.norm(vec2)
    return dot_product / (norm_vec1 * norm_vec2)

# Function to load embeddings from file
def load_embeddings(file_path):
    embeddings = {}
    with open(file_path, 'r') as f:
        for line in f:
            parts = line.strip().split()
            entity = parts[0]
            vector = np.array([float(x) for x in parts[1:]])
            embeddings[entity] = vector
    return embeddings

# Load department URIs from the JSON file
def load_departments_from_json(json_file):
    with open(json_file, 'r') as f:
        data = json.load(f)
    departments = [entry["instance"]["value"] for entry in data["results"]["bindings"]]
    return departments

# Function to compute cosine similarity matrix
def compute_department_similarity_matrix(embedding_file, json_file, output_file):
    embeddings = load_embeddings(embedding_file)
    departments = load_departments_from_json(json_file)
    
    n = len(departments)
    similarity_matrix = np.zeros((n, n))  # Initialize an n x n matrix
    
    # Compute cosine similarity for each pair
    for i, dept1 in enumerate(departments):
        if dept1 not in embeddings:
            continue
        for j, dept2 in enumerate(departments):
            if dept2 not in embeddings:
                continue
            if i <= j:  # Fill the matrix symmetrically
                similarity = cosine_similarity(embeddings[dept1], embeddings[dept2])
                similarity_matrix[i, j] = similarity
                similarity_matrix[j, i] = similarity  # Because cosine similarity is symmetric
    
    # Save to CSV
    with open(output_file, 'w') as f_out:
        writer = csv.writer(f_out)
        # Write header with department URIs
        writer.writerow([''] + departments)
        # Write each row of the similarity matrix
        for i, dept in enumerate(departments):
            writer.writerow([dept] + similarity_matrix[i].tolist())

# Specify paths to your files
embedding_file = '/home/dang/test/wang2vec/v200.txt'
json_file = 'rangeHeadOf.json'
output_file = 'headOfRange_similarity_matrix.csv'

# Run the similarity computation and save to CSV
compute_department_similarity_matrix(embedding_file, json_file, output_file)
