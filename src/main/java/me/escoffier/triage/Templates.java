package me.escoffier.triage;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.kohsuke.github.GHIssue;

import java.util.List;
import java.util.Map;

@CheckedTemplate
public class Templates {
    public static native TemplateInstance report(List<Project> projects, long count, String date);

    public static native TemplateInstance triageReport(List<Project> projects, long count, String date);
}