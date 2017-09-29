package drew.corenlp;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.File;

public class Neo4Connector implements AutoCloseable {
	private Driver driver;

	private GraphDatabaseService graphDataService;

	public Driver getDriver() {
		return driver;
	}

	public Neo4Connector(String uri, String user, String password) {
		// driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password
		// ) );

		File file = new File("C:/Users/s2876731.STAFF/Documents/Neo4jj");
		graphDataService = new GraphDatabaseFactory().newEmbeddedDatabase(file);
	}

	public GraphDatabaseService getNeo4jService() {
		return graphDataService;
	}

	@Override
	public void close() throws Exception {
		driver.close();
	}

	public void printGreeting(final String message) {
		try (Session session = driver.session()) {
			String greeting = session.writeTransaction(new TransactionWork<String>() {
				@Override
				public String execute(Transaction tx) {
					StatementResult result = tx.run("CREATE (a:Greeting) " + "SET a.message = $message "
							+ "RETURN a.message + ', from node ' + id(a)", parameters("message", message));
					return result.single().get(0).asString();
				}
			});
			System.out.println(greeting);
		}

	}
}