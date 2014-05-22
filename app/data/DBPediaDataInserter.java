package data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import models.Category;
import models.Choice;
import models.Question;
import models.QuizDAO;
import play.Logger;
import play.db.jpa.Transactional;
import at.ac.tuwien.big.we14.lab4.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we14.lab4.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we14.lab4.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we14.lab4.dbpedia.vocabulary.DBPediaOWL;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * DBPediaDataInserter used to fetch data from {@link DBPedia} converts it to
 * human readable form and saves to the DB.
 * 
 * @author lwnt
 * 
 */
public class DBPediaDataInserter {

	@Transactional
	public static void insertData() {

		if (!isAvailable()) {
			Logger.info("DBPedia is not available!");
			return;
		}

		Category category = buildQueries();

		if (category == null) {
			Logger.debug("Category is null");
			return;
		}

		QuizDAO.INSTANCE.persist(category);
		Logger.info("Data from DBPedia created!");
	}

	/**
	 * Build 5 queries, which creates {@link Choice}s and returns
	 * {@link Question}. Returned questions packed in a list and creates
	 * {@link Category} related to questions.
	 * 
	 * @return newly created {@link Category} object
	 */
	private static Category buildQueries() {

		List<Question> questions = new ArrayList<Question>();

		questions.add(queryOne());
		questions.add(queryTwo());
		questions.add(queryThree());
		questions.add(queryFour());
		questions.add(queryFive());

		if (questions.isEmpty()) {
			Logger.debug("Question list is empty!");
			return null;
		}

		Logger.info("Question list created with " + questions.size()
				+ " questions!");
		return createCategory(questions, "Europe", "Austria");
	}

	/**
	 * Creates a new {@link Category} object. Sets its name and questions.
	 * Relates also the questions to this category.
	 * 
	 * @param questions
	 *            a list of {@link Question} object, which belongs to this
	 *            category
	 * @param strings
	 *            name of newly created category. If there is more then one,
	 *            they will be separated by '/'.
	 * @return newly created {@link Category} object
	 */
	private static Category createCategory(List<Question> questions,
			String... strings) {

		Category category = new Category();

		StringBuilder nameEN = new StringBuilder();
		StringBuilder nameDE = new StringBuilder();

		for (int i = 0; i < strings.length; i++) {

			Resource tmp = getResourceFor(strings[i]);
			Text text = getTextFor(tmp);
			nameEN.append(text.getEnText() + " / ");
			nameDE.append(text.getDeText() + " / ");
		}

		category.setNameEN(nameEN.substring(0, nameEN.length() - 3));
		category.setNameDE(nameDE.substring(0, nameDE.length() - 3));
		category.setQuestions(questions);

		for (Question q : questions)
			q.setCategory(category);

		Logger.info("Category " + category.getNameEN() + " created!");
		return category;
	}

	/**
	 * Creates a new {@link Question} object. Sets its texts, maximum time and
	 * choice list, that belongs to this question. Relates also the questions in
	 * list to newly created question.
	 * 
	 * @param maxtime
	 *            maximum time for the question
	 * @param text
	 *            {@link Text} object which contains English and German text
	 * @param choices
	 *            a list of {@link Choice} objects, that belongs to this
	 *            Question
	 * @return newly created {@link Question} object
	 */
	private static Question createQuestion(BigDecimal maxtime, Text text,
			List<Choice> choices) {

		Question question = new Question();
		question.setMaxTime(maxtime);
		question.setTextEN(text.getEnText());
		question.setTextDE(text.getDeText());
		question.setChoices(choices);

		for (Choice c : choices)
			c.setQuestion(question);

		Logger.info("Question created!");

		return question;
	}

	/**
	 * Creates a list of newly created {@link Choice} objects. Shuffles the list
	 * and returns it.
	 * 
	 * @param trueTextList
	 *            a list of {@link Text} objects for correct choices.
	 * @param falseTextList
	 *            a list of {@link Text} objects for wrong choices.
	 * @return newly created and shuffled list
	 */
	private static List<Choice> createChoiceList(List<Text> trueTextList,
			List<Text> falseTextList) {

		List<Choice> choiceList = new ArrayList<Choice>();

		for (Text text : trueTextList)
			choiceList.add(createChoice(text, true));

		for (Text text : falseTextList)
			choiceList.add(createChoice(text, false));

		Logger.info("Choice list created with " + choiceList.size()
				+ " choices!");
		Collections.shuffle(choiceList);
		return choiceList;
	}

	/**
	 * Creates a new {@link Choice} object. Sets its text and if it's correct
	 * answer.
	 * 
	 * @param text
	 *            {@link Text} object which contains English and German texts
	 * @param correctAnswer
	 *            true if Choice is correct, false otherwise
	 * @return newly created {@link Choice} object
	 */
	private static Choice createChoice(Text text, boolean correctAnswer) {

		Choice choice = new Choice();
		choice.setTextEN(text.getEnText());
		choice.setTextDE(text.getDeText());
		choice.setCorrectAnswer(correctAnswer);
		return choice;
	}

	/**
	 * Creates a query to request data from {@link DBPedia}, which will return 4
	 * correct and 4 wrong answers.
	 * 
	 * Query: Cities in Austria and not in Germany.
	 * 
	 * @return newly created Question related the query
	 */
	private static Question queryOne() {

		Logger.info("Query 1 start!");

		Resource austria = getResourceFor("Austria");
		Resource germany = getResourceFor("Germany");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(4).addWhereClause(RDF.type, DBPediaOWL.City)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.country, austria)
				.addFilterClause(RDFS.label, Locale.ENGLISH)
				.addFilterClause(RDFS.label, Locale.GERMAN);

		Model citiesInAustria = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.country, austria);
		sqb.addWhereClause(DBPediaOWL.country, germany);

		Model citiesInGermany = getModelFor(sqb);

		List<Text> citiesInAustriaTexts = getTextsFor(citiesInAustria);
		List<Text> citiesInGermanyTexts = getTextsFor(citiesInGermany);

		List<Choice> choices = createChoiceList(citiesInAustriaTexts,
				citiesInGermanyTexts);

		if (choices.isEmpty()) {
			Logger.debug("Choice list is empty!");
			return null;
		}

		Text questionText = new Text();

		questionText.setEnText("Which Cities are located in "
				+ getTextFor(austria).getEnText() + "?");
		questionText.setDeText("Welche Staetde befinden sich in "
				+ getTextFor(austria).getDeText() + "?");

		return createQuestion(BigDecimal.valueOf(30), questionText, choices);
	}

	/**
	 * Creates a query to request data from {@link DBPedia}, which will return 4
	 * correct and 4 wrong answers.
	 * 
	 * Query: Countries, which uses euro as currency.
	 * 
	 * @return newly created Question related the query
	 */
	private static Question queryTwo() {

		Logger.info("Query 2 start!");

		Resource euro = getResourceFor("Euro");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(4).addWhereClause(RDF.type, DBPediaOWL.Country)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.currency, euro)
				.addFilterClause(RDFS.label, Locale.ENGLISH)
				.addFilterClause(RDFS.label, Locale.GERMAN);

		Model countriesUsingEuro = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.currency, euro);
		sqb.addMinusClause(DBPediaOWL.currency, euro);

		Model countriesNotUsingEuro = getModelFor(sqb);

		List<Text> countriesUsingEuroTexts = getTextsFor(countriesUsingEuro);
		List<Text> countriesNotUsingEuroTexts = getTextsFor(countriesNotUsingEuro);

		List<Choice> choices = createChoiceList(countriesUsingEuroTexts,
				countriesNotUsingEuroTexts);

		if (choices.isEmpty()) {
			Logger.debug("Choice list is empty!");
			return null;
		}

		Text questionText = new Text();

		questionText.setEnText("Which Countries are using "
				+ getTextFor(euro).getEnText() + " as currency?");
		questionText.setDeText("Welche Laender verwenden die Waehrung "
				+ getTextFor(euro).getDeText() + "?");

		return createQuestion(BigDecimal.valueOf(30), questionText, choices);
	}

	/**
	 * Creates a query to request data from {@link DBPedia}, which will return 4
	 * correct and 4 wrong answers.
	 * 
	 * Query: Rivers passing through Austria and not through Germany.
	 * 
	 * @return newly created Question related the query
	 */
	private static Question queryThree() {

		Logger.info("Query 3 start!");

		Resource austria = getResourceFor("Austria");
		Resource germany = getResourceFor("Germany");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(4).addWhereClause(RDF.type, DBPediaOWL.River)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.country, austria)
				.addFilterClause(RDFS.label, Locale.ENGLISH)
				.addFilterClause(RDFS.label, Locale.GERMAN);

		Model riversThroughAustria = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.country, austria);
		sqb.addWhereClause(DBPediaOWL.country, germany);

		Model riversThroughGermany = getModelFor(sqb);

		List<Text> riversThroughAustriaTexts = getTextsFor(riversThroughAustria);
		List<Text> riversThroughGermanyTexts = getTextsFor(riversThroughGermany);

		List<Choice> choices = createChoiceList(riversThroughAustriaTexts,
				riversThroughGermanyTexts);

		if (choices.isEmpty()) {
			Logger.debug("Choice list is empty!");
			return null;
		}

		Text questionText = new Text();

		questionText.setEnText("Which rivers pass through "
				+ getTextFor(austria).getEnText() + "?");
		questionText.setDeText("Welche Fluesse fliessen durch "
				+ getTextFor(austria).getDeText() + "?");

		return createQuestion(BigDecimal.valueOf(30), questionText, choices);
	}

	/**
	 * Creates a query to request data from {@link DBPedia}, which will return 4
	 * correct and 4 wrong answers.
	 * 
	 * Query: Lakes in Austria and not in Switzerland.
	 * 
	 * @return newly created Question related the query
	 */
	private static Question queryFour() {

		Logger.info("Query 4 start!");

		Resource austria = getResourceFor("Austria");
		Resource switzerland = getResourceFor("Switzerland");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(4).addWhereClause(RDF.type, DBPediaOWL.Lake)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.country, austria)
				.addFilterClause(RDFS.label, Locale.ENGLISH)
				.addFilterClause(RDFS.label, Locale.GERMAN);

		Model lakesInAustria = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.country, austria);
		sqb.addWhereClause(DBPediaOWL.country, switzerland);

		Model lakesInSwitzerland = getModelFor(sqb);

		List<Text> lakesInAustriaTexts = getTextsFor(lakesInAustria);
		List<Text> lakesInSwitzerlandTexts = getTextsFor(lakesInSwitzerland);

		List<Choice> choices = createChoiceList(lakesInAustriaTexts,
				lakesInSwitzerlandTexts);

		if (choices.isEmpty()) {
			Logger.debug("Choice list is empty!");
			return null;
		}

		Text questionText = new Text();

		questionText.setEnText("Which lakes are located in "
				+ getTextFor(austria).getEnText() + "?");
		questionText.setDeText("Welche Seen sind in "
				+ getTextFor(austria).getDeText() + "?");

		return createQuestion(BigDecimal.valueOf(30), questionText, choices);
	}

	/**
	 * Creates a query to request data from {@link DBPedia}, which will return 4
	 * correct and 4 wrong answers.
	 * 
	 * Query: Rivers sources at Austria and not in Switzerland.
	 * 
	 * @return newly created Question related the query
	 */
	private static Question queryFive() {

		Logger.info("Query 5 start!");

		Resource austria = getResourceFor("Austria");
		Resource switzerland = getResourceFor("Switzerland");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(4).addWhereClause(RDF.type, DBPediaOWL.River)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.sourceCountry, austria)
				.addFilterClause(RDFS.label, Locale.ENGLISH)
				.addFilterClause(RDFS.label, Locale.GERMAN);

		Model riversFromAustria = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.sourceCountry, austria);
		sqb.addWhereClause(DBPediaOWL.sourceCountry, switzerland);

		Model riversFromswitzerland = getModelFor(sqb);

		List<Text> riversFromAustriaTexts = getTextsFor(riversFromAustria);
		List<Text> riversFromSwitzerlandTexts = getTextsFor(riversFromswitzerland);

		List<Choice> choices = createChoiceList(riversFromAustriaTexts,
				riversFromSwitzerlandTexts);

		if (choices.isEmpty()) {
			Logger.debug("Choice list is empty!");
			return null;
		}

		Text questionText = new Text();

		questionText.setEnText("Which rivers sourced at "
				+ getTextFor(austria).getEnText() + "?");
		questionText.setDeText("Welche Fluesse stammen aus "
				+ getTextFor(austria).getDeText() + "?");

		return createQuestion(BigDecimal.valueOf(30), questionText, choices);
	}

	/**
	 * Checks if {@link DBPediaService} is available.
	 * 
	 * @return true if available, false otherwise
	 */
	private static boolean isAvailable() {
		return DBPediaService.isAvailable();
	}

	/**
	 * Creates a {@link Resource} from {@link DBPediaService} for the given
	 * {@link String} and returns it.
	 * 
	 * @param resourceString
	 *            String for the {@link Resource}
	 * @return retrieved {@link Resource}
	 */
	private static Resource getResourceFor(String resourceString) {
		return DBPediaService.loadStatements(DBPedia
				.createResource(resourceString));
	}

	/**
	 * Creates a {@link Model} from {@link DBPediaService} for the given
	 * {@link SelectQueryBuilder} and returns it.
	 * 
	 * @param sqb
	 *            {@link SelectQueryBuilder} from which the {@link Model}
	 *            created
	 * @return retrieved {@link Model}
	 */
	private static Model getModelFor(SelectQueryBuilder sqb) {
		return DBPediaService.loadStatements(sqb.toQueryString());
	}

	/**
	 * Gets English and German text of given {@link Resource}.
	 * 
	 * @param resource
	 *            {@link Resource} from which texts should be retrieved
	 * @return created {@link Text} object
	 */
	private static Text getTextFor(Resource resource) {

		Text text = new Text();
		text.setEnText(DBPediaService.getResourceName(resource, Locale.ENGLISH));
		text.setDeText(DBPediaService.getResourceName(resource, Locale.GERMAN));

		return text;
	}

	/**
	 * Gets English and German texts for given {@link Model}, creates
	 * {@link Text} object and packs them in a list.
	 * 
	 * @param model
	 *            {@link Model} from which the texts should be retrieved
	 * @return newly created list of texts
	 */
	private static List<Text> getTextsFor(Model model) {

		List<Text> texts = new ArrayList<Text>();
		List<String> en = DBPediaService
				.getResourceNames(model, Locale.ENGLISH);
		List<String> de = DBPediaService.getResourceNames(model, Locale.GERMAN);

		for (int i = 0; i < en.size(); i++)
			texts.add(new Text(en.get(i), de.get(i)));

		return texts;
	}

	/**
	 * Helper class, which contains only English and German Strings.
	 * 
	 * @author lwnt
	 * 
	 */
	private static class Text {

		private String enText;
		private String deText;

		Text() {

			this.enText = "unknown";
			this.deText = "unbekannt";
		}

		Text(String enText, String deText) {

			this.enText = enText;
			this.deText = deText;
		}

		public String getEnText() {
			return enText;
		}

		public void setEnText(String enText) {
			this.enText = enText;
		}

		public String getDeText() {
			return deText;
		}

		public void setDeText(String deText) {
			this.deText = deText;
		}
	}
}
