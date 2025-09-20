package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenPackageResponse(
    @JsonProperty("id")
    String id,
    
    @JsonProperty("name")
    String name,
    
    @JsonProperty("description")
    String description,
    
    @JsonProperty("tokens")
    Integer tokens,
    
    @JsonProperty("price")
    Double price,
    
    @JsonProperty("currency")
    String currency,
    
    @JsonProperty("discount")
    Double discount,
    
    @JsonProperty("isPopular")
    Boolean isPopular,
    
    @JsonProperty("features")
    java.util.List<String> features
) {
    
    public static TokenPackageResponse create(String id, String name, String description, 
                                           Integer tokens, Double price, String currency, 
                                           Double discount, Boolean isPopular, 
                                           java.util.List<String> features) {
        return new TokenPackageResponse(id, name, description, tokens, price, currency, 
                                      discount, isPopular, features);
    }
}
