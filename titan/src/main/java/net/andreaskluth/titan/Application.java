package net.andreaskluth.titan;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.tinkerpop.blueprints.Vertex;

public class Application {

  private static final String OWNS = "owns";

  private static final String NAME = "name";

  private static Logger LOG = LoggerFactory.getLogger(Application.class);

  private static final String INDEX_NAME = "search";
  private static final String DIRECTORY = "./db";

  public static void main(String[] args) {
    TitanFactory.Builder config = createConfiguration();
    TitanGraph graph = config.open();

    if (!isDatabaseAvailible()) {
      createDatabaseIfNotPresent(graph);
      createSampleData(graph);
    }

    Optional<Vertex> root = queryWithPropertyIndex(graph, "Microsoft");
    queryFromVertex(root.get());

    graph.shutdown();
  }

  private static Optional<Vertex> queryWithPropertyIndex(TitanGraph graph, String name) {
    Iterator<Vertex> vertices = graph.getVertices(NAME, name).iterator();
    if (vertices.hasNext()) {
      Vertex root = vertices.next();
      LOG.info("Found the root " + root.getProperty(NAME));
      return Optional.of(root);
    }

    return Optional.empty();
  }

  private static void queryFromVertex(Vertex parent) {
    Iterable<Vertex> vertices = parent.query().labels(OWNS).vertices();
    for (Vertex vertex : vertices) {
      LOG.info("Subsidaries are " + vertex.getProperty(NAME));
    }
  }

  private static boolean isDatabaseAvailible() {
    return new File(DIRECTORY).exists();
  }

  private static void createDatabaseIfNotPresent(TitanGraph graph) {
    TitanManagement mgmt = graph.getManagementSystem();
    PropertyKey name = mgmt.makePropertyKey(NAME).dataType(String.class).make();
    mgmt.buildIndex("byName", Vertex.class).addKey(name).buildCompositeIndex();
    mgmt.commit();
  }

  private static void createSampleData(TitanGraph graph) {
    Vertex microsoft = graph.addVertex(null);
    microsoft.setProperty(NAME, "Microsoft");

    Vertex nokia = graph.addVertex(null);
    nokia.setProperty(NAME, "NOKIA");

    Vertex apiphany = graph.addVertex(null);
    apiphany.setProperty(NAME, "Apiphany");

    graph.addEdge(null, microsoft, nokia, OWNS);
    graph.addEdge(null, microsoft, apiphany, OWNS);

    graph.commit();
  }

  private static TitanFactory.Builder createConfiguration() {
    TitanFactory.Builder config = TitanFactory.build();
    config.set("storage.backend", "berkeleyje");
    config.set("storage.directory", DIRECTORY);
    config.set("index." + INDEX_NAME + ".backend", "elasticsearch");
    config.set("index." + INDEX_NAME + ".directory", DIRECTORY + File.separator + "es");
    config.set("index." + INDEX_NAME + ".elasticsearch.local-mode", true);
    config.set("index." + INDEX_NAME + ".elasticsearch.client-only", false);
    return config;
  }

}
