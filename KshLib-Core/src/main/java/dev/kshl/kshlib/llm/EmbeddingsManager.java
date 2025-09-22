package dev.kshl.kshlib.llm;

import dev.kshl.kshlib.exceptions.BusyException;
import dev.kshl.kshlib.llm.embed.FloatEmbeddings;
import dev.kshl.kshlib.log.ILogger;
import dev.kshl.kshlib.misc.FileUtil;
import dev.kshl.kshlib.sql.EmbeddingsDAO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class EmbeddingsManager {
    private final int contextLength;
    private final OllamaAPI ollamaAPI;
    private final EmbeddingsDAO embeddingsDAO;
    private final ILogger logger;
    private final int tokensPerBlock;
    private final File rootDirectory;
    private static final float CHARS_PER_TOKEN = 3.5f;

    public EmbeddingsManager(int contextLength, OllamaAPI ollamaAPI, EmbeddingsDAO embeddingsDAO, ILogger logger, int tokensPerBlock, File rootDirectory) {
        this.contextLength = contextLength;
        this.ollamaAPI = ollamaAPI;
        this.embeddingsDAO = embeddingsDAO;
        this.logger = logger;
        this.tokensPerBlock = tokensPerBlock;
        this.rootDirectory = rootDirectory;
    }

    public void setEmbedFiles() throws SQLException, BusyException, IOException {
        List<File> files = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(rootDirectory.toPath())) {
            paths.forEach(path -> {
                File file = path.toFile();
                if (file.isDirectory()) return;
                files.add(file);
            });
        }

        Set<String> paths = new HashSet<>();
        for (File file : files) {
            paths.add(getPathRelativeToWorkingDirectory(file));
        }
        for (String file : embeddingsDAO.getFiles()) {
            if (!paths.contains(file)) {
                logger.info("Dropping old file " + file);
                embeddingsDAO.dropFile(file);
            }
        }

        ConcurrentLinkedQueue<File> queue = new ConcurrentLinkedQueue<>(files);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> {
                File file;
                while ((file = queue.poll()) != null) {
                    try {
                        updateFile(file);
                    } catch (Throwable e) {
                        logger.print("An error occurred embedding file " + file.getName(), e);
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updateFile(File file) throws SQLException, BusyException, IOException {
        String path = getPathRelativeToWorkingDirectory(file);
        byte[] currentHash = FileUtil.getSHA256Hash(file.getCanonicalPath());
        byte[] databaseHash = embeddingsDAO.getFileHash(path);
        boolean sameHash = Arrays.equals(currentHash, databaseHash);
        String msg = "Checking file " + path + "... ";
        if (databaseHash == null) {
            msg += "file not in database, embedding file.";
            logger.info(msg);
        } else if (!sameHash) {
            msg += "different hash, re-embedding file.";
            logger.info(msg);
        }
        if (sameHash) return;
        // New or changed file
        embeddingsDAO.dropFile(path);

        String content = FileUtil.read(file);
        if (content == null) throw new FileNotFoundException(path);
        int startIndex = 0;
        int fileLength = content.length();
        int defaultLength = Math.round(Math.min(contextLength, tokensPerBlock) * CHARS_PER_TOKEN * 0.8f); // Expect to use approximately 80% of context length
        int charOverlap = Math.round(100 * CHARS_PER_TOKEN); // 100 token overlap between blocks

        int blockIndex = 1;

        for (int endIndex = defaultLength; ; endIndex += defaultLength) {
            if (blockIndex == 1 && fileLength < contextLength * CHARS_PER_TOKEN * 0.75f) {
                endIndex = Integer.MAX_VALUE; // If the entire file can fit in 75% of the context, just embed the entire file.
            }
            String block;
            EmbedResponse response;
            for (int i = 0; ; i++) {
                if (i > 9 || endIndex <= startIndex) {
                    throw new IllegalStateException("Unable to find small enough block. Context length must be incorrect, likely by at least a factor of 10.");
                }
                block = "/" + path + (endIndex == Integer.MAX_VALUE ? "" : ("[" + blockIndex + "]")) + "\n\n";
                if (blockIndex > 1) {
                    block += "... ";
                }
                block += content.substring(startIndex, Math.min(fileLength, endIndex));
                if (endIndex < fileLength) {
                    block += " ...";
                }
                response = getEmbeddingsDocument(path, "search_document: " + block);

                if (response.prompt_eval_count() < contextLength) break; // Ensures nothing was truncated

                logger.warning(path + String.format(" Too many tokens. start=%s, end=%s, block.len=%s", startIndex, endIndex, block.length()));
                if (endIndex == Integer.MAX_VALUE) endIndex = defaultLength; // If embedding the entire file fails, go back to trying blocks.
                else endIndex -= Math.round(defaultLength * 0.1f); // Roll back the endIndex to have fewer tokens
            }

            if (!(response.embeddings() instanceof FloatEmbeddings floatEmbeddings)) {
                throw new IllegalArgumentException("Unexpected embeddings: " + response.embeddings().getClass().getName());
            }

            embeddingsDAO.upsertFileBlock(path, path.split("/")[1], startIndex, endIndex, block, floatEmbeddings.getEmbeddings());

            if (endIndex >= fileLength) break;
            startIndex = endIndex - charOverlap;
            blockIndex++;
        }

        if (embeddingsDAO.upsertFileHash(path, currentHash)) { // Done at the end in-case it doesn't complete
            logger.info("Saved hash");
        } else {
            logger.warning("Failed to upsert hash");
        }
    }

    public EmbedResponse getEmbeddingsDocument(String fileName, String text) throws IOException {
        return ollamaAPI.embeddings(GemmaEmbedRequest.document(fileName, text));
    }

    public EmbedResponse getEmbeddingsQuery(String text) throws IOException {
        return ollamaAPI.embeddings(GemmaEmbedRequest.query(text));
    }

    public float[] getEmbeddingsFloatQuery(String text) throws IOException {
        var response = getEmbeddingsQuery("search_query: " + text);
        if (!(response.embeddings() instanceof FloatEmbeddings floatEmbeddings)) {
            throw new IllegalArgumentException("Unexpected embeddings: " + response.embeddings().getClass().getName());
        }
        return floatEmbeddings.getEmbeddings();
    }

    private String getPathRelativeToWorkingDirectory(File file) throws IOException {
        Path rootPath = rootDirectory.toPath().toRealPath();
        Path filePath = file.toPath().toRealPath();
        return rootPath.relativize(filePath).toString();
    }
}
