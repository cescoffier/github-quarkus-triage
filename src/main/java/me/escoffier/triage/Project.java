package me.escoffier.triage;

import java.util.List;
import java.util.Map;

public class Project {

    public final String name;

    public final Map<String, List<Issue>> issues;

    public Project(String name, Map<String, List<Issue>> issues) {
        this.name = name;
        this.issues = issues;
    }
}
