package drew.corenlp;

import java.io.File;

import org.bson.Document;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoConnector {

	private MongoClient mongoClient;
	private MongoDatabase database;

	public MongoConnector(String url, int port, String databaseName) {
		// Mongo Initialization
		mongoClient = new MongoClient(url, port);
		database = mongoClient.getDatabase(databaseName);

	}

	public void closeDB() {
		mongoClient.close();
	}

	public MongoDatabase getDatabase() {
		return database;
	}
}
