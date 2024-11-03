package com.rory.assuredtest.parser;

import lombok.Data;

import java.util.List;

@Data
public class ConfigInfo {
    private String kmsAppName;
    private String gdtApplicationId;
    private List<String> prompts;
    private List<DomainInfo> domains;
    
    // Getters and Setters

    @Data
    public static class DomainInfo {
        private String domainName;
        private List<String> domainCodes;
        private List<RoleInfo> roles;
        private int priority;
        
        // Getters and Setters
    }
    @Data
    public static class RoleInfo {
        private String roleName;
        private String type;
        private String adGroup;
        
        // Getters and Setters
    }
}
