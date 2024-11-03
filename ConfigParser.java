package com.rory.assuredtest.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rory.assuredtest.parser.ConfigInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigParser {

    private final JsonNode jsonRoot;

    public ConfigParser(String jsonFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.jsonRoot = mapper.readTree(new File(jsonFilePath));
    }

    public List<ConfigInfo> getConfigInfoByAdGroups(List<String> adGroupNames) {
        List<ConfigInfo> results = new ArrayList<>();

        // 遍历 JSON 根节点中的每个对象
        for (JsonNode node : jsonRoot) {
            String loginGroup = node.path("loginGroup").asText();

            // 如果 loginGroup 在 adGroupNames 中，则继续查找配置
            if (adGroupNames.contains(loginGroup)) {
                ConfigInfo config = new ConfigInfo();
                config.setKmsAppName(node.path("kmsAppName").asText());
                config.setGdtApplicationId(node.path("gdtApplicationId").asText());
                config.setPrompts(parsePrompts(node));

                // 查找符合条件的 domains
                List<ConfigInfo.DomainInfo> domains = findDomains(node, adGroupNames);
                if (!domains.isEmpty()) {
                    config.setDomains(domains);
                    results.add(config);
                }
            }
        }
        return results;
    }

    private List<String> parsePrompts(JsonNode node) {
        List<String> prompts = new ArrayList<>();
        node.path("prompts").forEach(prompt -> prompts.add(prompt.asText()));
        return prompts;
    }

    private List<ConfigInfo.DomainInfo> findDomains(JsonNode node, List<String> adGroupNames) {
        List<ConfigInfo.DomainInfo> matchedDomains = new ArrayList<>();

        node.path("domains").forEach(domain -> {
            ConfigInfo.DomainInfo domainInfo = new ConfigInfo.DomainInfo();
            domainInfo.setDomainName(domain.path("name").asText());
            domainInfo.setPriority(domain.path("priority").asInt());

            // 获取domainCodes并检查approvedDomains
            domainInfo.setDomainCodes(parseDomainCodes(domain, node));

            List<ConfigInfo.RoleInfo> roles = findRoles(domain, adGroupNames);
            if (!roles.isEmpty()) {
                domainInfo.setRoles(roles);
                matchedDomains.add(domainInfo);
            }
        });

        return matchedDomains.stream()
                .sorted(Comparator.comparingInt(d -> d.getPriority())) // 根据 priority 排序
                .collect(Collectors.toList());
    }

    private List<String> parseDomainCodes(JsonNode domain, JsonNode parentNode) {
        List<String> domainCodes = new ArrayList<>();

        // 添加当前domain的domainCodes
        domain.path("domainCodes").forEach(code -> domainCodes.add(code.asText()));

        // 如果approvedDomains不为空，则查找其对应的domainCodes
        if (domain.path("approvedDomains").size() > 0) {
            domain.path("approvedDomains").forEach(approvedDomain -> {
                String approvedDomainName = approvedDomain.asText();
                // 遍历整个JSON对象查找具有相同domainName的domain
                parentNode.path("domains").forEach(d -> {
                    if (d.path("name").asText().equals(approvedDomainName)) {
                        d.path("domainCodes").forEach(code -> domainCodes.add(code.asText()));
                    }
                });
            });
        }
        return domainCodes;
    }

    private List<ConfigInfo.RoleInfo> findRoles(JsonNode domain, List<String> adGroupNames) {
        List<ConfigInfo.RoleInfo> roles = new ArrayList<>();

        domain.path("roles").forEach(role -> {
            String adGroup = role.path("adGroup").asText();
            if (adGroupNames.contains(adGroup)) {
                ConfigInfo.RoleInfo roleInfo = new ConfigInfo.RoleInfo();
                roleInfo.setRoleName(role.path("name").asText());
                roleInfo.setType(role.path("type").asText());
                roleInfo.setAdGroup(adGroup);
                roles.add(roleInfo);
            }
        });

        return roles;
    }
}
