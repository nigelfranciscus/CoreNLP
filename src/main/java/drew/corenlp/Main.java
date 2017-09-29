package drew.corenlp;

import java.io.IOException;

import com.mongodb.MongoClient;

public class Main {
	public static void main(String[] argc){
		
		Neo4Connector neo4jConnector = new Neo4Connector("bolt://localhost:7687", "neo4j", "ultrasafe");
		MongoConnector mongoConnector = new MongoConnector("localhost", 27017, "twitter");

		DependencyTreeConverter dtConverter = new DependencyTreeConverter(neo4jConnector.getNeo4jService(), mongoConnector.getDatabase());
		try {
			dtConverter.convertTwitt();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
}
