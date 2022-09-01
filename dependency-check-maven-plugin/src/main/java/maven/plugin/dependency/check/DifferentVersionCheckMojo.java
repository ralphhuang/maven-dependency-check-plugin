package maven.plugin.dependency.check;

import maven.plugin.dependency.check.domain.WarnLevel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.*;


/**
 * check whether exist that one dependency has different versions and print dependency tree
 * @deprecated it's not work while maven solve this by default.
 */
@Deprecated
//@Mojo(name = "version", requiresDependencyCollection = ResolutionScope.TEST, threadSafe = true)
public class DifferentVersionCheckMojo extends AbstractDependencyCheckMojo {


    @Override
    public void execute() throws MojoExecutionException {

        Log log = getLog();

        DependencyNode rootNode = buildDependencyRootNode();

        if (skip) {
            log.warn(addLogPrefix("skip different versions check:"));
            return;
        }

        //check
        Map<String, Set<DependencyNode>> map = new TreeMap<>();
        findDifferentVersions(rootNode, map);

        //remove from map which has only one version and get a new Map after filter
        Map<String, Set<DependencyNode>> nodeMap = remove(map);


        //print to console
        if (nodeMap.isEmpty()) {
            log.info(addLogPrefix("no different version dependency found:"));
        } else {
            log.error(addLogPrefix("different version found blow:"));

            StringBuilder sb = new StringBuilder();
            sb.append("\n\r");
            sb.append("------------------------------------------------------------------------------");
            sb.append("\n\r");
            nodeMap.forEach((key, value) -> {
                sb.append("--> \t").append(key).append("\n\r");
                value.forEach(node -> sb.append("---->\t").append(node.getArtifact().toString()).append("\n\r"));
            });
            sb.append("------------------------------------------------------------------------------");
            log.info(sb.toString());

            if (WarnLevel.ERROR == warnLevel) {
                throw new MojoExecutionException(addLogPrefix("different version found,exist maven execution"));
            }
        }
    }

    private Map<String, Set<DependencyNode>> remove(Map<String, Set<DependencyNode>> map) {

        if (map == null || map.size() == 0) {
            return map;
        }

        Map<String, Set<DependencyNode>> copy = new TreeMap<>();

        map.forEach((key, value) -> {
            if (value != null && value.size() > 1) {
                copy.put(key, value);
            }
        });

        return copy;
    }

    private void findDifferentVersions(DependencyNode node, Map<String, Set<DependencyNode>> map) {

        List<DependencyNode> children = node.getChildren();

        if (children != null && children.size() > 0) {
            children.forEach(child -> {

                //current node
                String key = child.getArtifact().getGroupId() + ":" + child.getArtifact().getArtifactId();
                Set<DependencyNode> set = map.get(key);
                if (set == null) {
                    set = new TreeSet<>(VersionComparator.singleton());
                }
                set.add(child);
                map.put(key, set);

                //child node
                findDifferentVersions(child, map);

            });
        }
    }

    /**
     * Comparator
     */
    static class VersionComparator implements Comparator<DependencyNode> {

        private static final Comparator<DependencyNode> INSTANCE = new VersionComparator();

        public static Comparator<DependencyNode> singleton() {
            return INSTANCE;
        }

        private VersionComparator() {
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

            return o1.getArtifact().getVersion().compareTo(o2.getArtifact().getVersion());
        }
    }
}