package com.publication_trend_tracking_system.sever_web_app.util;

import java.util.*;

public class CosineSimilarityUtil {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "about", "as", "into", "like", "through", "after", "over", "between", "out",
            "against", "during", "without", "before", "under", "around", "among", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "shall", "should", "can", "could", "may", "might", "must", "it",
            "this", "that", "these", "those", "we", "they", "he", "she", "which", "who", "whom",
            "what", "where", "when", "why", "how", "from"
    ));

    public static double calculateCosineSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        Map<String, Integer> wordCount1 = getWordFrequencies(text1);
        Map<String, Integer> wordCount2 = getWordFrequencies(text2);
        
        Set<String> uniqueWords = new HashSet<>();
        uniqueWords.addAll(wordCount1.keySet());
        uniqueWords.addAll(wordCount2.keySet());
        
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        for (String word : uniqueWords) {
            int freq1 = wordCount1.getOrDefault(word, 0);
            int freq2 = wordCount2.getOrDefault(word, 0);
            
            dotProduct += (freq1 * freq2);
            magnitude1 += (freq1 * freq1);
            magnitude2 += (freq2 * freq2);
        }
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private static Map<String, Integer> getWordFrequencies(String text) {
        Map<String, Integer> wordFrequencies = new HashMap<>();
        // Simple tokenization: lowercase, remove punctuation, split by whitespace
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9]", " ").split("\\s+");
        
        for (String word : words) {
            if (!word.isBlank() && !STOP_WORDS.contains(word) && word.length() > 2) {
                wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0) + 1);
            }
        }
        return wordFrequencies;
    }
    
    public static List<String> extractTopKeywords(String text, int limit) {
        Map<String, Integer> freqs = getWordFrequencies(text);
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(freqs.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue())); // Descending
        
        List<String> topKeywords = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedList.size()); i++) {
            topKeywords.add(sortedList.get(i).getKey());
        }
        return topKeywords;
    }
}
