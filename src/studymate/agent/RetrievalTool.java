package studymate.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import studymate.model.Chunk;
import studymate.model.Material;
import studymate.repository.StudyRepository;

public class RetrievalTool implements Tool {

    private static final int MAX_RESULTS = 8;

    private static final Set<String> STOP_WORDS =
            new HashSet<>(Arrays.asList(
                    "a",
                    "an",
                    "and",
                    "are",
                    "as",
                    "at",
                    "be",
                    "by",
                    "can",
                    "do",
                    "does",
                    "for",
                    "from",
                    "how",
                    "i",
                    "in",
                    "is",
                    "it",
                    "of",
                    "on",
                    "or",
                    "that",
                    "the",
                    "this",
                    "to",
                    "was",
                    "what",
                    "when",
                    "where",
                    "which",
                    "who",
                    "why",
                    "with"
            ));

    private final StudyRepository repository;

    public RetrievalTool(
            StudyRepository repository) {

        this.repository = repository;
    }

    @Override
    public ToolResult execute(
            Map<String, String> arguments) {

        String courseIdText =
                arguments.get("courseId");

        String query =
                arguments.getOrDefault(
                        "query",
                        ""
                );

        if (courseIdText == null
                || courseIdText.isBlank()) {

            return new ToolResult(
                    "Missing course ID.",
                    List.of()
            );
        }

        UUID courseId;

        try {

            courseId =
                    UUID.fromString(
                            courseIdText
                    );

        } catch (IllegalArgumentException e) {

            return new ToolResult(
                    "Invalid course ID.",
                    List.of()
            );
        }

        List<Material> materials =
                repository
                        .getMaterialsByCourse(
                                courseId
                        );

        if (materials.isEmpty()) {

            return new ToolResult(
                    "No materials are available "
                            + "for this course.",
                    List.of()
            );
        }

        if (query == null
                || query.isBlank()) {

            return retrieveAll(
                    materials
            );
        }

        return retrieveRelevant(
                materials,
                query
        );
    }

    // =====================================================
    // F02 / F03 - ALL COURSE EVIDENCE
    // =====================================================

    private ToolResult retrieveAll(
            List<Material> materials) {

        StringBuilder evidence =
                new StringBuilder();

        List<UUID> sourceIds =
                new ArrayList<>();

        for (Material material :
                materials) {

            for (Chunk chunk :
                    material.chunks()) {

                addChunk(
                        evidence,
                        sourceIds,
                        material,
                        chunk
                );
            }
        }

        return new ToolResult(
                evidence.toString(),
                sourceIds
        );
    }

    // =====================================================
    // F04 - RELEVANT QUESTION EVIDENCE
    // =====================================================

    private ToolResult retrieveRelevant(
            List<Material> materials,
            String query) {

        List<String> queryWords =
                tokenizeToList(query);

        if (queryWords.isEmpty()) {

            return new ToolResult(
                    "No meaningful search terms "
                            + "were found.",
                    List.of()
            );
        }

        Map<String, Integer> documentFrequency =
                calculateDocumentFrequency(
                        materials,
                        queryWords
                );

        int totalChunks =
                countChunks(materials);

        List<ScoredChunk> scoredChunks =
                new ArrayList<>();

        for (Material material :
                materials) {

            for (Chunk chunk :
                    material.chunks()) {

                ScoreResult scoreResult =
                        calculateScore(
                                chunk.getText(),
                                query,
                                queryWords,
                                documentFrequency,
                                totalChunks
                        );

                if (scoreResult.score() > 0) {

                    scoredChunks.add(
                            new ScoredChunk(
                                    material,
                                    chunk,
                                    scoreResult.score(),
                                    scoreResult.matchedTerms()
                            )
                    );
                }
            }
        }

        scoredChunks.sort(
                Comparator
                        .comparingInt(
                                ScoredChunk::score
                        )
                        .reversed()
                        .thenComparing(
                                Comparator
                                        .comparingInt(
                                                ScoredChunk::matchedTerms
                                        )
                                        .reversed()
                        )
        );

        if (scoredChunks.isEmpty()) {

            return new ToolResult(
                    "No matching evidence found "
                            + "for the question.",
                    List.of()
            );
        }

        StringBuilder evidence =
                new StringBuilder();

        List<UUID> sourceIds =
                new ArrayList<>();

        int resultCount =
                Math.min(
                        MAX_RESULTS,
                        scoredChunks.size()
                );

        for (int i = 0;
             i < resultCount;
             i++) {

            ScoredChunk result =
                    scoredChunks.get(i);

            addChunk(
                    evidence,
                    sourceIds,
                    result.material(),
                    result.chunk()
            );
        }

        return new ToolResult(
                evidence.toString(),
                sourceIds
        );
    }

    // =====================================================
    // RELEVANCE SCORING
    // =====================================================

    private ScoreResult calculateScore(
            String text,
            String query,
            List<String> queryWords,
            Map<String, Integer> documentFrequency,
            int totalChunks) {

        String normalizedText =
                normalize(text);

        String normalizedQuery =
                normalize(query);

        List<String> textWordList =
                tokenizeToList(text);

        Map<String, Integer> termCounts =
                countTerms(textWordList);

        int score = 0;
        int matchedTerms = 0;

        // Bonus if the complete question appears.
        if (normalizedText.contains(
                normalizedQuery)) {

            score += 50;
        }

        // Bonus for consecutive important terms.
        for (int i = 0;
             i < queryWords.size() - 1;
             i++) {

            String first =
                    queryWords.get(i);

            String second =
                    queryWords.get(i + 1);

            List<String> stemmedTextWords =
                    textWordList;

            for (int j = 0;
                 j < stemmedTextWords.size() - 1;
                 j++) {

                if (stemmedTextWords.get(j)
                        .equals(first)
                        && stemmedTextWords
                                .get(j + 1)
                                .equals(second)) {

                    score += 25;
                }
            }
        }

        Set<String> uniqueQueryWords =
                new HashSet<>(
                        queryWords
                );

        for (String word :
                uniqueQueryWords) {

            int occurrences =
                    termCounts
                            .getOrDefault(
                                    word,
                                    0
                            );

            if (occurrences > 0) {

                matchedTerms++;

                int frequency =
                        documentFrequency
                                .getOrDefault(
                                        word,
                                        totalChunks
                                );

                double rarity =
                        Math.log(
                                (totalChunks + 1.0)
                                        / (frequency + 1.0)
                        );

                int wordScore =
                        4
                                + (int) Math.round(
                                        rarity * 6
                                );

                // Repeated important words provide
                // additional evidence.
                int occurrenceBonus =
                        Math.min(
                                occurrences,
                                5
                        );

                score +=
                        wordScore
                                * occurrenceBonus;
            }
        }

        return new ScoreResult(
                score,
                matchedTerms
        );
    }

    // =====================================================
    // TERM COUNTS
    // =====================================================

    private Map<String, Integer> countTerms(
            List<String> words) {

        Map<String, Integer> counts =
                new HashMap<>();

        for (String word :
                words) {

            counts.put(
                    word,
                    counts.getOrDefault(
                            word,
                            0
                    ) + 1
            );
        }

        return counts;
    }

    // =====================================================
    // DOCUMENT FREQUENCY
    // =====================================================

    private Map<String, Integer>
            calculateDocumentFrequency(
                    List<Material> materials,
                    List<String> queryWords) {

        Map<String, Integer> frequencies =
                new HashMap<>();

        Set<String> uniqueQueryWords =
                new HashSet<>(
                        queryWords
                );

        for (Material material :
                materials) {

            for (Chunk chunk :
                    material.chunks()) {

                Set<String> chunkWords =
                        new HashSet<>(
                                tokenizeToList(
                                        chunk.getText()
                                )
                        );

                for (String word :
                        uniqueQueryWords) {

                    if (chunkWords.contains(word)) {

                        frequencies.put(
                                word,
                                frequencies
                                        .getOrDefault(
                                                word,
                                                0
                                        )
                                        + 1
                        );
                    }
                }
            }
        }

        return frequencies;
    }

    // =====================================================
    // CHUNK COUNT
    // =====================================================

    private int countChunks(
            List<Material> materials) {

        int count = 0;

        for (Material material :
                materials) {

            count +=
                    material
                            .chunks()
                            .size();
        }

        return count;
    }

    // =====================================================
    // TOKENIZATION + SIMPLE STEMMING
    // =====================================================

    private List<String> tokenizeToList(
            String text) {

        List<String> words =
                new ArrayList<>();

        String normalized =
                normalize(text);

        if (normalized.isBlank()) {

            return words;
        }

        String[] tokens =
                normalized.split("\\s+");

        for (String token :
                tokens) {

            if (!token.isBlank()
                    && token.length() > 1
                    && !STOP_WORDS
                            .contains(token)) {

                words.add(
                        stem(token)
                );
            }
        }

        return words;
    }

    private String stem(
            String word) {

        // Simple plural handling:
        // patterns -> pattern
        // strategies -> strategy

        if (word.endsWith("ies")
                && word.length() > 4) {

            return word.substring(
                    0,
                    word.length() - 3
            ) + "y";
        }

        if (word.endsWith("s")
                && word.length() > 3
                && !word.endsWith("ss")) {

            return word.substring(
                    0,
                    word.length() - 1
            );
        }

        return word;
    }

    private String normalize(
            String text) {

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .trim();
    }

    // =====================================================
    // EVIDENCE OUTPUT
    // =====================================================

    private void addChunk(
            StringBuilder evidence,
            List<UUID> sourceIds,
            Material material,
            Chunk chunk) {

        sourceIds.add(
                chunk.getId()
        );

        evidence.append(
                "[SOURCE "
                        + chunk.getId()
                        + " | "
                        + material.getTitle()
                        + " | "
                        + chunk.sourceRef()
                        + "]"
        );

        evidence.append("\n");

        evidence.append(
                chunk.getText()
        );

        evidence.append("\n\n");
    }

    // =====================================================
    // INTERNAL TYPES
    // =====================================================

    private record ScoreResult(
            int score,
            int matchedTerms) {
    }

    private record ScoredChunk(
            Material material,
            Chunk chunk,
            int score,
            int matchedTerms) {
    }
}