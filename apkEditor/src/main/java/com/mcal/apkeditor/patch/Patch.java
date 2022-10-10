package com.mcal.apkeditor.patch;

import java.util.ArrayList;
import java.util.List;

public class Patch {
    public final int engineVersion = 1;
    public int requiredEngine;
    public String author;
    public String description;
    public String packagename;
    public List<PatchRule> rules = new ArrayList<>();

    public Patch() {

    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRequiredEngine() {
        return requiredEngine;
    }

    public void setRequiredEngine(int requiredEngine) {
        this.requiredEngine = requiredEngine;
    }

    public String getPackageName() {
        return packagename;
    }

    public void setPackageName(String packageName) {
        this.packagename = packageName;
    }

    public List<PatchRule> getRules() {
        return rules;
    }

    public void setRule(PatchRule rule) {
        this.rules.add(rule);
    }

    public String getVersion() {
        return "1.1";
    }
}
