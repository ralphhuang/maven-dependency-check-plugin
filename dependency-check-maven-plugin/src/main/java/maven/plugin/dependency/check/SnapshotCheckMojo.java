package maven.plugin.dependency.check;

import maven.plugin.dependency.check.domain.WarnLevel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.*;


/**
 * check whether exist SNAPSHOT dependencies and print dependency tree
 */
@Mojo(name = "snapshot", requiresDependencyCollection = ResolutionScope.TEST, threadSafe = true)
public class SnapshotCheckMojo extends AbstractDependencyCheckMojo {

    private static final String SNAPSHOT = "-SNAPSHOT";

    @Override
    public void execute() throws MojoExecutionException {

        Log log = getLog();

        DependencyNode rootNode = buildDependencyRootNode();

        if (skip) {
            log.warn(addLogPrefix("skip SNAPSHOT dependency check:"));
            return;
        }

        //check SNAPSHOT dependencies
        List<DependencyNode> snapShotsNodes = new ArrayList<>(findSubSnapshot(rootNode));

        //sort and remove duplicate
        Set<DependencyNode> snapShotsNodeSet = new TreeSet<>(DependencyNodeComparator.singleton());
        snapShotsNodeSet.addAll(snapShotsNodes);

        //print to console
        if (snapShotsNodeSet.isEmpty()) {
            log.info(addLogPrefix("no SNAPSHOT dependency found:"));
        } else {
            log.error(addLogPrefix("SNAPSHOT dependencies found blow:"));

            StringBuilder sb = new StringBuilder();
            sb.append("\n\r");
            sb.append("------------------------------------------------------------------------------");
            sb.append("\n\r");
            snapShotsNodeSet.forEach(c -> sb.append("-->\t").append(c.getArtifact().toString()).append("\n\r"));
            sb.append("------------------------------------------------------------------------------");
            log.info(sb.toString());

            if (WarnLevel.ERROR == warnLevel) {
                throw new MojoExecutionException(addLogPrefix("SNAPSHOT dependencies found,exist maven execution"));
            }
        }
    }

    private Set<DependencyNode> findSubSnapshot(DependencyNode node) {

        Set<DependencyNode> set = new HashSet<>();

        //skip when the module it's self is SNAPSHOT version
        if (node.getParent() != null && node.getArtifact().getBaseVersion().endsWith(SNAPSHOT)) {
            set.add(node);
        }

        List<DependencyNode> children = node.getChildren();

        if (children != null) {
            children.forEach(child -> {
                Set<DependencyNode> r = findSubSnapshot(child);
                if (!r.isEmpty()) {
                    set.addAll(r);
                }
            });
        }

        return set;

    }


    /**
     * Comparator
     */
    static class DependencyNodeComparator implements Comparator<DependencyNode> {

        private static final Comparator<DependencyNode> INSTANCE = new DependencyNodeComparator();

        public static Comparator<DependencyNode> singleton() {
            return INSTANCE;
        }

        private DependencyNodeComparator() {
        }

        @Override
        public int compare(DependencyNode o1, DependencyNode o2) {

            if (o1 == o2) {
                return 0;
            }

            if (o1 == null) {
                return -1;
            }

            if (o2 == null) {
                return 1;
            }

            return o1.getArtifact().toString().compareTo(o2.getArtifact().toString());
        }
    }
}