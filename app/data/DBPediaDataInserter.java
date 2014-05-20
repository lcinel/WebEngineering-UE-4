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

	/**
	 * Checks if {@link DBPediaService} is available, if it is, creates
	 * {@link Resource}s and {@link SelectQueryBuilder}s to fetch data from
	 * {@link DBPedia} into {@link Model}s. It creates then corresponding
	 * {@link Category}, {@link Question}s and {@link Choice}s and persists them
	 * in to DB.
	 */
	@play.db.jpa.Transactional
	public static void insertData() {

		if (!isAvailable()) {
			Logger.info("DBPedia is not available!");
			return;
		}

		Category category = new Category();
		
		Resource europe = getResourceFor("Europe");
		Resource austria = getResourceFor("Austria");
		
		category.setNameEN(getTextFor(europe).getEnText() + " / " + getTextFor(austria).getEnText());
		category.setNameDE(getTextFor(europe).getDeText() + " / " + getTextFor(austria).getDeText());
		
		List<Question> questions = createQuestionList();

		for(Question q : questions)
			q.setCategory(category);
		
		category.setQuestions(questions);
		
		QuizDAO.INSTANCE.persist(category);
		
		Logger.info("Data from DBPedia inserted: ");
	}

	private static List<Question> createQuestionList() {

		List<Question> questions = new ArrayList<Question>();

		questions.add(questionOne());
		questions.add(questionTwo());
		questions.add(questionThree());
		questions.add(questionFour());
		questions.add(questionFive());
		
		return questions;
	}

	/**
	 * Creates a {@link Question}, sets maximum time, English and German texts
	 * of it and returns it. Note that this method does not assign choices of
	 * the {@link Question}!
	 * 
	 * @param maxtime
	 *            maximum time
	 * @param textEN
	 *            English text
	 * @param textDE
	 *            German text
	 * @return created {@link Question} itself
	 */
	private static Question createQuestion(BigDecimal maxtime) {

		Question question = new Question();
		question.setMaxTime(maxtime);

		return question;
	}

	/**
	 * Creates {@link Choice}s, packs them into an {@link ArrayList} and returns
	 * it.
	 * 
	 * @param question
	 *            for which the {@link Choice}s belong
	 * @param trueENTextList
	 *            list of English texts for correct answers
	 * @param trueDETextList
	 *            list of German texts for correct answers
	 * @param falseENTextList
	 *            list of English texts for wrong answers
	 * @param falseDETextList
	 *            list of German texts for wrong answers
	 * @return created {@link ArrayList}
	 */
	private static void createChoiceList(Question question,
			List<Text> trueTextList, List<Text> falseTextList) {

		List<Choice> choiceList = new ArrayList<Choice>();
		int sizeTrue = trueTextList.size();

		for (int i = 0; i < sizeTrue; i++) {

			Choice choice = createChoice(question, trueTextList.get(i), true);
			choiceList.add(choice);
		}

		int sizeFalse = falseTextList.size();

		for (int i = 0; i < sizeFalse; i++) {

			Choice choice = createChoice(question, falseTextList.get(i), false);
			choiceList.add(choice);
		}

		Collections.shuffle(choiceList);
		question.setChoices(choiceList);
	}

	/**
	 * Creates a {@link Choice}, sets its {@link Question}, English and German
	 * text of it and if it's the correct answer and returns it.
	 * 
	 * @param question
	 *            to which the {@link Choice} belongs
	 * @param textEN
	 *            English text
	 * @param textDE
	 *            German text
	 * @param correctAnswer
	 *            if it's the correct answer
	 * @return created {@link Choice} itself
	 */
	private static Choice createChoice(Question question, Text text,
			boolean correctAnswer) {

		Choice choice = new Choice();
		choice.setQuestion(question);
		choice.setTextEN(text.getEnText());
		choice.setTextDE(text.getDeText());
		choice.setCorrectAnswer(correctAnswer);
		return choice;
	}

	private static Question questionOne() {

		Question question = createQuestion(BigDecimal.valueOf(30));

		queryOne(question);

		return question;
	}

	private static void queryOne(Question question) {

		Resource austria = getResourceFor("Austria");
		Resource germany = getResourceFor("Germany");
		
		String questionTextEn = "Which Cities are located in " + getTextFor(austria).getEnText() +"?";
		String questionTextDe = "Welche Staetde befinden sich in " + getTextFor(austria).getDeText() + "?";
		
		question.setTextEN(questionTextEn);
		question.setTextDE(questionTextDe);

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

		createChoiceList(question, citiesInAustriaTexts, citiesInGermanyTexts);
	}
	
	private static Question questionTwo() {

		Question question = createQuestion(BigDecimal.valueOf(40));

		queryTwo(question);

		return question;
	}
	
	private static void queryTwo(Question question) {

		Resource euro = getResourceFor("Euro");
		
		String questionTextEn = "Which Countries are using " + getTextFor(euro).getEnText() +" as currency?";
		String questionTextDe = "Welche Laender verwenden die Waehrung " + getTextFor(euro).getDeText() + "?";
		
		question.setTextEN(questionTextEn);
		question.setTextDE(questionTextDe);

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

		createChoiceList(question, countriesUsingEuroTexts, countriesNotUsingEuroTexts);
	}
	
	private static Question questionThree() {

		Question question = createQuestion(BigDecimal.valueOf(40));

		queryThree(question);

		return question;
	}
	
	private static void queryThree(Question question) {

		Resource austria = getResourceFor("Austria");
		Resource germany = getResourceFor("Germany");
		
		String questionTextEn = "Which rivers pass through " + getTextFor(austria).getEnText() +"?";
		String questionTextDe = "Welche Fluesse fliessen durch " + getTextFor(austria).getDeText() + "?";
		
		question.setTextEN(questionTextEn);
		question.setTextDE(questionTextDe);

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

		createChoiceList(question, riversThroughAustriaTexts, riversThroughGermanyTexts);
	}
	
	private static Question questionFour() {

		Question question = createQuestion(BigDecimal.valueOf(35));

		queryFour(question);

		return question;
	}
	
	private static void queryFour(Question question) {

		Resource austria = getResourceFor("Austria");
		Resource switzerland = getResourceFor("Switzerland");
		
		String questionTextEn = "Which lakes are located in " + getTextFor(austria).getEnText() +"?";
		String questionTextDe = "Welche Seen sind in " + getTextFor(austria).getDeText() + "?";
		
		question.setTextEN(questionTextEn);
		question.setTextDE(questionTextDe);

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

		createChoiceList(question, lakesInAustriaTexts, lakesInSwitzerlandTexts);
	}
	
	private static Question questionFive() {

		Question question = createQuestion(BigDecimal.valueOf(50));

		queryFive(question);

		return question;
	}
	
	private static void queryFive(Question question) {

		Resource austria = getResourceFor("Austria");
		Resource switzerland = getResourceFor("Switzerland");
		
		String questionTextEn = "Which rivers sourced at " + getTextFor(austria).getEnText() +"?";
		String questionTextDe = "Welche Fluesse stammen aus " + getTextFor(austria).getDeText() + "?";
		
		question.setTextEN(questionTextEn);
		question.setTextDE(questionTextDe);

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

		createChoiceList(question, riversFromAustriaTexts, riversFromSwitzerlandTexts);
	}

	/**
	 * Checks if {@link DBPedia} is available.
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
	 * @return created {@link Resource}
	 */
	private static Resource getResourceFor(String resourceString) {

		return DBPediaService.loadStatements(DBPedia.createResource(resourceString));
	}

	/**
	 * Creates a {@link Model} from {@link DBPediaService} for the given
	 * {@link SelectQueryBuilder} and returns it.
	 * 
	 * @param sqb
	 *            {@link SelectQueryBuilder} from which the {@link Model}
	 *            created
	 * @return created {@link Model}
	 */
	private static Model getModelFor(SelectQueryBuilder sqb) {

		return DBPediaService.loadStatements(sqb.toQueryString());
	}

	/**
	 * Gets the English name of the given {@link Resource} and returns it.
	 * 
	 * @param resource
	 *            name requested from
	 * @return name of the {@link Resource}
	 */
	private static Text getTextFor(Resource resource) {

		Text text = new Text();
		text.setEnText(DBPediaService.getResourceName(resource, Locale.ENGLISH));
		text.setDeText(DBPediaService.getResourceName(resource, Locale.GERMAN));

		return text;
	}

	private static List<Text> getTextsFor(Model model) {

		List<Text> texts = new ArrayList<Text>();
		List<String> en = DBPediaService.getResourceNames(model, Locale.ENGLISH);
		List<String> de = DBPediaService.getResourceNames(model, Locale.GERMAN);

		for (int i = 0; i < en.size(); i++)
			texts.add(new Text(en.get(i), de.get(i)));

		return texts;
	}

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
