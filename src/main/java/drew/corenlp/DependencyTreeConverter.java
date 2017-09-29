package drew.corenlp;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
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
import edu.stanford.nlp.util.CoreMap;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import org.json.JSONObject;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

/**
 * A simple corenlp example ripped directly from the Stanford CoreNLP website
 * using text from wikinews.
 */
public class DependencyTreeConverter {

	private GraphDatabaseService neo4jService;
	private MongoDatabase mongoDatabase;

	public DependencyTreeConverter(GraphDatabaseService p_neo4jService, MongoDatabase p_mongoDatabase) {
		neo4jService = p_neo4jService;
		mongoDatabase = p_mongoDatabase;
	}

	public void convertTwitt() throws IOException {

		MongoCollection<Document> collection = mongoDatabase.getCollection("nlp");

		for (Document cur : collection.find().projection(new Document("_id", 0).append("text", 1))) {
			String text = cur.toJson();
			JSONObject jsonObj = new JSONObject(text);
			String textValue = jsonObj.getString("text").replaceAll("https?://\\S+\\s?", "").replaceAll("\\P{Print}",
					"");
			System.out.println(textValue);

			// creates a StanfordCoreNLP object, with POS tagging,
			// lemmatization, NER, parsing, and coreference resolution
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

			// create an empty Annotation just with the given text
			Annotation document = new Annotation(textValue);

			// run all Annotators on this text
			pipeline.annotate(document);

			// these are all the sentences in this document
			// a CoreMap is essentially a Map that uses class objects as keys
			// and has values with custom types
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			for (CoreMap sentence : sentences) {

				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
				List<IndexedWord> twitWords = dependencies.getAllNodesByWordPattern(".*");

				try (Transaction tx = neo4jService.beginTx()) {
					for (IndexedWord word : twitWords) {
						Node existingNode = neo4jService.findNode(DynamicLabel.label("word"), "text",
								word.originalText());
						if (existingNode != null) {
							// skip the dulicate word
							continue;
						}

						Node wordNode = neo4jService.createNode();
						wordNode.addLabel(DynamicLabel.label("word"));
						wordNode.setProperty("text", word.originalText());
						wordNode.setProperty("tag", word.tag());

					}
					tx.success();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try (Transaction tx = neo4jService.beginTx()) {
					for (IndexedWord word1 : twitWords) {

						@SuppressWarnings("deprecation")
						Node word1Node = neo4jService.findNode(DynamicLabel.label("word"), "text",
								word1.originalText());

						if (word1Node != null) {
							for (IndexedWord word2 : twitWords) {
								SemanticGraphEdge edge = dependencies.getEdge(word1, word2);
								if (edge != null) {
									Iterable<Relationship> relationShips = word1Node.getRelationships();
									Node word2Node = neo4jService.findNode(DynamicLabel.label("word"), "text",
											word2.originalText());

									if (word2Node != null) {

										boolean skip = false;
										for (Relationship relationShip : relationShips) {
											if (relationShip.getEndNode().equals(word2Node)) {
												relationShip.setProperty("frequency",
														(Integer) relationShip.getProperty("frequency") + 1);
												skip = true;
												break;
											}
										}

										if (skip) {
											continue;
										}

										Relationship newRelation = word1Node.createRelationshipTo(word2Node,
												DynamicRelationshipType.withName("_"));
										newRelation.setProperty("frequency", 1);

									}

								}
							}
						}
					}
					tx.success();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		}
	}
}
