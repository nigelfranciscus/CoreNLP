package drew.corenlp;

import java.io.File;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import com.google.common.io.Files;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoClient;
import org.bson.Document;

/*import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBCursor;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;*/

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;

import org.neo4j.driver.v1.*;

/**
 * A simple corenlp example ripped directly from the Stanford CoreNLP website
 * using text from wikinews.
 */
public class SimpleExample {

	public static void main(String[] args) throws IOException {

		// Pretty printing JSON
		ObjectMapper mapper = new ObjectMapper();
		// System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cur));

		// Mongo Initialization
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase database = mongoClient.getDatabase("twitter");
		MongoCollection<Document> collection = database.getCollection("nlp");

		for (Document cur : collection.find().projection(new Document("_id", 0).append("text", 1))) {
			String text = cur.toJson();
			JSONObject jsonObj = new JSONObject(text);
			String textValue = jsonObj.getString("text").replaceAll("https?://\\S+\\s?", "").replaceAll("\\P{Print}",
					"");
			System.out.println(textValue);

			// creates a StanfordCoreNLP object, with POS tagging,
			// lemmatization,
			// NER, parsing, and coreference resolution
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			// read some text from the file..
			// File inputFile = new
			// File("src/test/resources/sample-content.txt");
			// String text = Files.toString(inputFile,
			// Charset.forName("UTF-8"));

			// create an empty Annotation just with the given text
			Annotation document = new Annotation(textValue);

			// run all Annotators on this text
			pipeline.annotate(document);

			// these are all the sentences in this document
			// a CoreMap is essentially a Map that uses class objects as keys
			// and
			// has values with custom types
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			for (CoreMap sentence : sentences) {
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional token-specific
				// methods
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class);
					// this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class);
					// this is the NER label of the token
					String ne = token.get(NamedEntityTagAnnotation.class);

					// System.out.println("word: " + word + " pos: " + pos + "
					// ne:" + ne);
				}

				// this is the parse tree of the current sentence
				// Tree tree = sentence.get(TreeAnnotation.class);
				// System.out.println("parse tree:\n" + tree);

				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				//System.out.println("dependency graph:\n" + dependencies);

				/*
				 * The IndexedWord object is very similar to the CoreLabel
				 * object only is used in the SemanticGraph context
				 */				
				IndexedWord firstRoot = dependencies.getFirstRoot();
				
				
				/*List<SemanticGraphEdge> incomingEdgesSorted = dependencies.getIncomingEdgesSorted(firstRoot);

				for (SemanticGraphEdge edge : incomingEdgesSorted) {
					// Getting the target node with attached edges
					IndexedWord dep = edge.getDependent();

					// Getting the source node with attached edges
					IndexedWord gov = edge.getGovernor();

					// Get the relation name between them
					GrammaticalRelation relation = edge.getRelation();
				}*/

				// this section is same as above just we retrieve the OutEdges
				List<SemanticGraphEdge> outEdgesSorted = dependencies.getOutEdgesSorted(firstRoot);
			        
				for (SemanticGraphEdge edge : outEdgesSorted) {
					
					IndexedWord dep = edge.getDependent();
					System.out.println("Dependent: " + dep);
					
					IndexedWord gov = edge.getGovernor();
					System.out.println("Governor: " + gov);
					
					GrammaticalRelation relation = edge.getRelation();
					System.out.println("Relation: " + relation + "\n");
				}

			}

			// This is the coreference link graph
			// Each chain stores a set of mentions that link to each other,
			// along with a method for getting the most representative mention
			// Both sentence and token offsets start at 1!
			// Map<Integer, CorefChain> graph =
			// document.get(CorefChainAnnotation.class);
		}
		
		//neo4j
		
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "ultrasafe" ) );
		

		mongoClient.close();

	}

}
