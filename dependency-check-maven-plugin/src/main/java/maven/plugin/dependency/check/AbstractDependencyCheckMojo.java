package maven.plugin.dependency.check;

import maven.plugin.dependency.check.domain.WarnLevel;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.SerializingDependencyNodeVisitor;

import java.io.StringWriter;

public abstract class AbstractDependencyCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component(hint = "default")
    protected DependencyCollectorBuilder dependencyCollectorBuilder;

    /**
     * when set in pom.xml,plugin will be skip
     * maven.dependency.check.skip = true
     */
    @Parameter(defaultValue = "${maven.dependency.check.skip}", name = "skip")
    protected boolean skip;

    /**
     * warnLevel
     */
    @Parameter(name = "warnLevel")
    protected WarnLevel warnLevel;

    /**
     * whether to print dependency in tree view,default set to false
     * maven.dependency.check.printTree = false
     */
    @Parameter(name = "printTree", defaultValue = "${maven.dependency.check.printTree}")
    protected boolean printTree;

    /**
     * plugin-name-display
     */
    private static final String PLUGIN_NAME = "maven-dependency-check-plugin";


    /**
     * build DependencyRootNode for current module
     *
     * @return DependencyNode
     */
    protected DependencyNode buildDependencyRootNode() throws MojoExecutionException {

        Log log = getLog();

        try {
            //get build session from maven build phase
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);

            //build DependencyRootNode
            DependencyNode rootNode = dependencyCollectorBuilder.collectDependencyGraph(buildingRequest, null);

            log.info(addLogPrefix("printTree: " + printTree));
            if (printTree) {
                String dependencyTreeString = serializeDependencyTree(rootNode);
                log.info(addLogPrefix("maven dependency tree:\n" + dependencyTreeString));
            }

            return rootNode;

        } catch (DependencyCollectorBuilderException e) {
            // should not happen
            log.error(addLogPrefix("buildDependencyRootNode error"), e);
            throw new MojoExecutionException(addLogPrefix("buildDependencyRootNode error"), e);
        }
    }

    /**
     * Serializes the specified dependency tree to a string.
     *
     * @param theRootNode the dependency tree root node to serialize
     * @return the serialized dependency tree
     */
    private String serializeDependencyTree(DependencyNode theRootNode) {
        StringWriter writer = new StringWriter();

        DependencyNodeVisitor visitor = new SerializingDependencyNodeVisitor(writer, SerializingDependencyNodeVisitor.STANDARD_TOKENS);
        visitor = new BuildingDependencyNodeVisitor(visitor);
        theRootNode.accept(visitor);

        return writer.toString();
    }

    protected String addLogPrefix(String msg) {
        return PLUGIN_NAME + ": " + msg;
    }
}