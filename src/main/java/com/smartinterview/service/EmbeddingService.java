package com.smartinterview.service;

public interface EmbeddingService {
    float[] embed(String text);
    String toJson(float[] vector);
    float[] fromJson(String json);
    double cosineSimilarity(float[] a, float[] b);

}
