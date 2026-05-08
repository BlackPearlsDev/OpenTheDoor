package openthedoor.scan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class ScanResult {
    private final Set<String> urls = new TreeSet<>();
    private final Set<String> domains = new TreeSet<>();
    private final Set<String> paths = new TreeSet<>();
    private final Set<String> protocolHints = new TreeSet<>();
    private final Set<String> interestingFiles = new TreeSet<>();

    public void addUrls(Set<String> values) { urls.addAll(values); }
    public void addDomains(Set<String> values) { domains.addAll(values); }
    public void addPaths(Set<String> values) { paths.addAll(values); }
    public void addHint(String value) { protocolHints.add(value); }
    public void addInterestingFile(String value) { interestingFiles.add(value); }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# OpenTheDoor APK scan report\n\n");
        appendSet(sb, "URLs found", urls);
        appendSet(sb, "Domains found", domains);
        appendSet(sb, "Interesting paths", paths);
        appendSet(sb, "Protocol hints", protocolHints);
        appendSet(sb, "Interesting files", interestingFiles);

        sb.append("## Suggested hosts entries\n\n");
        if (domains.isEmpty()) {
            sb.append("No domain found.\n\n");
        } else {
            sb.append("```txt\n");
            for (String d : domains) sb.append("127.0.0.1 ").append(d).append('\n');
            sb.append("```\n\n");
        }

        sb.append("## Suggested OpenTheDoor config\n\n");
        sb.append("```properties\n");
        sb.append("mode=http-listen\n");
        sb.append("host=0.0.0.0\n");
        sb.append("port=80\n");
        sb.append("mockDir=mocks\n");
        sb.append("savePackets=true\n");
        sb.append("logDir=logs\n");
        sb.append("```\n");
        return sb.toString();
    }

    public void writeMarkdown(String outputPath) throws IOException {
        File out = new File(outputPath);
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(out)) {
            writer.write(toMarkdown());
        }
    }

    public String toConsoleString() {
        return toMarkdown();
    }

    private void appendSet(StringBuilder sb, String title, Set<String> values) {
        sb.append("## ").append(title).append("\n\n");
        if (values.isEmpty()) {
            sb.append("None found.\n\n");
            return;
        }
        for (String value : values) sb.append("- `").append(value).append("`\n");
        sb.append('\n');
    }
}
