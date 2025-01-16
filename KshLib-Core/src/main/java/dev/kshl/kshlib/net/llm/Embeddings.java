package dev.kshl.kshlib.net.llm;

import dev.kshl.kshlib.json.JSONCollector;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class Embeddings {
    private final List<Float> embeddings;

    public Embeddings(List<Float> embeddings) {
        this.embeddings = embeddings.stream().toList();
    }

    public Embeddings(JSONArray jsonArray) {
        this(fromJSON(jsonArray));
    }

    public Embeddings(byte[] bytes) {
        List<Float> embeds = new ArrayList<>();
        for (int i = 0; i < bytes.length; i += 4) {
            int floatInt =
                    ((bytes[i] & 0xFF) << 24) |
                            ((bytes[i + 1] & 0xFF) << 16) |
                            ((bytes[i + 2] & 0xFF) << 8) |
                            (bytes[i + 3] & 0xFF);
            embeds.add(Float.intBitsToFloat(floatInt));
        }
        this.embeddings = Collections.unmodifiableList(embeds);
    }

    public static Embeddings fromNestedList(List<List<Float>> embeddings) {
        List<Float> combined = new ArrayList<>();
        for (List<Float> embedding : embeddings) {
            combined.addAll(embedding);
        }
        return new Embeddings(combined);
    }

    private static List<Float> fromJSON(JSONArray array) {
        List<Float> embeddings = new ArrayList<>();
        addAll(array, embeddings);
        return embeddings;
    }

    private static void addAll(JSONArray array, List<Float> embeddings) {
        for (Object o : array) {
            if (o instanceof JSONArray jsonArray) addAll(jsonArray, embeddings);
            else if (o instanceof BigDecimal bigDecimal) embeddings.add(bigDecimal.floatValue());
            else if (o instanceof Double d) embeddings.add(d.floatValue());
            else if (o instanceof Float f) embeddings.add(f);
            else if (o instanceof String s) embeddings.add(Float.parseFloat(s));
            else throw new IllegalArgumentException("Unexpected data type: " + o.getClass().getName());
        }
    }

    public JSONArray toJSON() {
        return this.embeddings.stream().collect(new JSONCollector());
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(getEmbeddings().size() * 4);
        for (Float embedding : getEmbeddings()) {
            buffer.putFloat(embedding);
        }
        return buffer.array();
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(getBytes());
    }

    public static Embeddings fromBase64(String base64) {
        return new Embeddings(Base64.getDecoder().decode(base64));
    }

    @Override
    public int hashCode() {
        return embeddings.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Embeddings otherEmbeddings)) return false;
        return this.embeddings.equals(otherEmbeddings.embeddings);
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public List<Float> getEmbeddings() {
        return this.embeddings;
    }

    /**
     * Calculates the cosine similarity between two embeddings.
     *
     * @param other The other Embeddings instance to compare with.
     * @return The cosine similarity between the two embeddings.
     * @throws IllegalArgumentException if the embeddings have different dimensions or are empty.
     */
    public double compareTo(Embeddings other) {
        if (this.embeddings.size() != other.embeddings.size()) {
            throw new IllegalArgumentException("Embeddings must have the same number of elements");
        }
        if (this.embeddings.isEmpty()) {
            throw new IllegalArgumentException("Embeddings cannot be empty");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < this.embeddings.size(); i++) {
            double a = this.embeddings.get(i);
            double b = other.embeddings.get(i);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("Embeddings cannot have zero norm");
        }

        return dotProduct / Math.sqrt(normA * normB);
    }

    /**
     * Truncates the vector after numComponents elements
     */
    public Embeddings getFirstN(int numComponents) {
        List<Float> embeddings = new ArrayList<>();
        for (Float embedding : getEmbeddings()) {
            embeddings.add(embedding);
            if (embeddings.size() >= numComponents) break;
        }
        return new Embeddings(embeddings);
    }

    /**
     * Applies PCA to reduce the dimensionality of the embeddings.
     *
     * @param numComponents The number of principal components to retain (e.g., 64).
     * @return A new Embeddings object with reduced dimensions.
     */
    public Embeddings applyPCA(int numComponents) {
        if (numComponents <= 0 || numComponents >= embeddings.size()) {
            throw new IllegalArgumentException("Number of components must be between 1 and " + (embeddings.size() - 1));
        }

        int d = embeddings.size(); // Original dimensionality

        // Step 1: Center the data
        float[] centeredData = new float[d];
        double mean = embeddings.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        for (int i = 0; i < d; i++) {
            centeredData[i] = embeddings.get(i) - (float) mean;
        }

        // Step 2: Compute the covariance matrix
        double[][] covarianceMatrix = new double[d][d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j <= i; j++) {
                double cov = centeredData[i] * centeredData[j];
                covarianceMatrix[i][j] = cov;
                covarianceMatrix[j][i] = cov;
            }
        }

        // Step 3: Compute the eigenvalues and eigenvectors of the covariance matrix
        EigenDecomposition eigenDecomp = new EigenDecomposition(covarianceMatrix);
        double[][] eigenVectors = eigenDecomp.getV();
        double[] eigenValues = eigenDecomp.getD();

        // Step 4: Sort eigenvalues and eigenvectors in descending order
        Integer[] indices = new Integer[d];
        for (int i = 0; i < d; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> Double.compare(eigenValues[b], eigenValues[a]));

        // Step 5: Select the top 'numComponents' eigenvectors
        double[][] selectedEigenVectors = new double[d][numComponents];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < numComponents; j++) {
                selectedEigenVectors[i][j] = eigenVectors[i][indices[j]];
            }
        }

        // Step 6: Transform the original data
        double[] transformedData = new double[numComponents];
        for (int i = 0; i < numComponents; i++) {
            for (int j = 0; j < d; j++) {
                transformedData[i] += centeredData[j] * selectedEigenVectors[j][i];
            }
        }

        // Convert the transformed data to a list of floats
        List<Float> reducedEmbeddings = new ArrayList<>();
        for (double value : transformedData) {
            reducedEmbeddings.add((float) value);
        }

        return new Embeddings(reducedEmbeddings);
    }

    /**
     * Helper class to store eigenvalues and eigenvectors.
     */
    private static class EigenDecomposition {
        private final double[] d; // Eigenvalues
        private final double[][] v; // Eigenvectors

        /**
         * Computes the eigenvalues and eigenvectors of a symmetric matrix using Jacobi's method.
         */
        public EigenDecomposition(double[][] matrix) {
            int n = matrix.length;
            double[] d = new double[n];
            double[][] v = new double[n][n];

            // Initialize V to the identity matrix
            for (int i = 0; i < n; i++) {
                v[i][i] = 1.0;
            }

            // Copy matrix to a temporary array
            double[][] a = new double[n][n];
            for (int i = 0; i < n; i++) {
                System.arraycopy(matrix[i], 0, a[i], 0, n);
            }

            // Initialize d with the diagonal of A
            for (int i = 0; i < n; i++) {
                d[i] = a[i][i];
            }

            int maxIterations = 100;
            double epsilon = 1e-6;

            for (int iter = 0; iter < maxIterations; iter++) {
                double sum = 0.0;
                for (int p = 0; p < n - 1; p++) {
                    for (int q = p + 1; q < n; q++) {
                        sum += Math.abs(a[p][q]);
                    }
                }

                if (sum < epsilon) {
                    break;
                }

                for (int p = 0; p < n - 1; p++) {
                    for (int q = p + 1; q < n; q++) {
                        double g = 100.0 * Math.abs(a[p][q]);
                        if (iter > 4 && Math.abs(d[p]) + g == Math.abs(d[p]) &&
                                Math.abs(d[q]) + g == Math.abs(d[q])) {
                            a[p][q] = 0.0;
                        } else if (Math.abs(a[p][q]) > epsilon) {
                            double h = d[q] - d[p];
                            double t;
                            if (Math.abs(h) + g == Math.abs(h)) {
                                t = a[p][q] / h;
                            } else {
                                double theta = 0.5 * h / a[p][q];
                                t = 1.0 / (Math.abs(theta) + Math.sqrt(1.0 + theta * theta));
                                if (theta < 0.0) {
                                    t = -t;
                                }
                            }

                            double c = 1.0 / Math.sqrt(1 + t * t);
                            double s = t * c;
                            double tau = s / (1.0 + c);

                            h = t * a[p][q];
                            d[p] -= h;
                            d[q] += h;
                            a[p][q] = 0.0;

                            for (int r = 0; r < p; r++) {
                                rotate(a, v, r, p, r, q, s, tau);
                            }
                            for (int r = p + 1; r < q; r++) {
                                rotate(a, v, p, r, r, q, s, tau);
                            }
                            for (int r = q + 1; r < n; r++) {
                                rotate(a, v, p, r, q, r, s, tau);
                            }
                        }
                    }
                }
            }

            this.d = d;
            this.v = v;
        }

        public double[] getD() {
            return d;
        }

        public double[][] getV() {
            return v;
        }

        /**
         * Rotates the matrix A and eigenvector matrix V.
         *
         * @param a The matrix to be rotated.
         * @param v The matrix of eigenvectors.
         * @param i Row index 1.
         * @param j Column index 1.
         * @param k Row index 2.
         * @param l Column index 2.
         * @param s Sine of the rotation angle.
         */
        private static void rotate(double[][] a, double[][] v, int i, int j, int k, int l, double s, double tau) {
            double g = a[i][j];
            double h = a[k][l];
            a[i][j] = g - s * (h + g * tau);
            a[k][l] = h + s * (g - h * tau);
            g = v[i][j];
            h = v[k][l];
            v[i][j] = g - s * (h + g * tau);
            v[k][l] = h + s * (g - h * tau);
        }
    }

}