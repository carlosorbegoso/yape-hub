package org.sky.service.billing;

import org.sky.dto.ApiResponse;
import org.sky.model.SubscriptionPlanEntity;
import org.sky.model.TokenPackageEntity;

import java.util.List;
import java.util.Map;

@jakarta.enterprise.context.ApplicationScoped
public class BillingResponseBuilder {

    public ApiResponse<List<Map<String, Object>>> buildPlansResponse(List<SubscriptionPlanEntity> plans) {
        List<Map<String, Object>> planList = plans.stream()
                .map(this::buildPlanMap)
                .toList();
        
            return ApiResponse.success("Plans retrieved successfully", planList);
    }

    public ApiResponse<List<Map<String, Object>>> buildTokenPackagesResponse(List<TokenPackageEntity> packages) {
        List<Map<String, Object>> tokenPackages = packages.stream()
                .map(this::buildTokenPackageMap)
                .toList();
        
            return ApiResponse.success("Token packages retrieved successfully", tokenPackages);
    }

    private Map<String, Object> buildPlanMap(SubscriptionPlanEntity plan) {
        Map<String, Object> planMap = new java.util.HashMap<>();
        planMap.put("id", plan.id);
        planMap.put("name", plan.name);
        planMap.put("description", plan.description != null ? plan.description : "");
        planMap.put("price", plan.pricePen.doubleValue());
        planMap.put("currency", "PEN");
        planMap.put("billingCycle", plan.billingCycle);
        planMap.put("maxAdmins", plan.maxAdmins);
        planMap.put("maxSellers", plan.maxSellers);
        planMap.put("tokensIncluded", plan.tokensIncluded);
        planMap.put("features", parseFeatures(plan.features));
        planMap.put("isActive", plan.isActive);
        planMap.put("createdAt", plan.createdAt != null ? plan.createdAt.toString() : "");
        return planMap;
    }

    private Map<String, Object> buildTokenPackageMap(TokenPackageEntity pkg) {
        Map<String, Object> packageMap = new java.util.HashMap<>();
        packageMap.put("id", pkg.packageId);
        packageMap.put("name", pkg.name);
        packageMap.put("description", pkg.description != null ? pkg.description : "");
        packageMap.put("tokens", pkg.tokens);
        packageMap.put("price", pkg.price.doubleValue());
        packageMap.put("currency", pkg.currency);
        packageMap.put("discount", pkg.discount.doubleValue());
        packageMap.put("isPopular", pkg.isPopular);
        packageMap.put("features", parseFeatures(pkg.features));
        packageMap.put("discountedPrice", pkg.getDiscountedPrice().doubleValue());
        return packageMap;
    }

    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            String cleaned = featuresJson.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
                return List.of(cleaned.split(","))
                        .stream()
                        .map(feature -> feature.trim().replaceAll("^\"|\"$", ""))
                        .toList();
            }
        } catch (Exception e) {
            // Log warning if needed
        }
        
        return List.of();
    }
}
