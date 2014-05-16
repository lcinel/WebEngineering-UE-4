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

		// TODO
		// Extract QueryBuilder to a method.
		// Query 5 Questions.
		// Check if it's better to use createQuestionList.

		if (!isAvailable()) {
			Logger.info("DBPedia is not available!");
			return;
		}

		Resource director = getResourceFor("Tim_Burton");
		Resource actor = getResourceFor("Johnny_Depp");

		SelectQueryBuilder sqb = DBPediaService.createQueryBuilder()
				.setLimit(2).addWhereClause(RDF.type, DBPediaOWL.Film)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.director, director)
				.addWhereClause(DBPediaOWL.starring, actor)
				.addFilterClause(RDFS.label, Locale.ENGLISH);

		Model jdMoviesByTB = getModelFor(sqb);

		sqb.removeWhereClause(DBPediaOWL.director, director);
		sqb.addMinusClause(DBPediaOWL.director, director);

		Model jdMoviesNotTB = getModelFor(sqb);

		String directorName = getEnName(director);
		String actorName = getEnName(actor);

		List<String> jdByTbNames = getEnNames(jdMoviesByTB);
		List<String> jdNoTbNames = getEnNames(jdMoviesNotTB);

		Category category = createCategory("Films", "Filme");
		Question question = createQuestion(BigDecimal.valueOf(30),
				"Question 1", "Frage 1");
		List<Question> questions = createQuestionList(question);
		List<Choice> choiceList = createChoiceList(question, jdByTbNames,
				jdByTbNames, jdNoTbNames, jdNoTbNames);

		question.setChoices(choiceList);
		question.setCategory(category);
		category.setQuestions(questions);

		QuizDAO.INSTANCE.persist(category);
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
	 * @param resource
	 *            String for the {@link Resource}
	 * @return created {@link Resource}
	 */
	private static Resource getResourceFor(String resource) {

		return DBPediaService.loadStatements(DBPedia.createResource(resource));
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
	private static String getEnName(Resource resource) {

		return DBPediaService.getResourceName(resource, Locale.ENGLISH);
	}

	/**
	 * Gets the German name of the given {@link Resource} and returns it.
	 * 
	 * @param resource
	 *            name requested from
	 * @return name of the {@link Resource}
	 */
	private static String getDeName(Resource resource) {

		return DBPediaService.getResourceName(resource, Locale.GERMAN);
	}

	/**
	 * Gets the English names of the given {@link Model} and returns them as a
	 * List.
	 * 
	 * @param model
	 *            names requested from
	 * @return List of names from {@link Model}
	 */
	private static List<String> getEnNames(Model model) {

		return DBPediaService.getResourceNames(model, Locale.ENGLISH);
	}

	/**
	 * Gets the German names of the given {@link Model} and returns them as a
	 * List.
	 * 
	 * @param model
	 *            names requested from
	 * @return List of names from {@link Model}
	 */
	private static List<String> getDeNames(Model model) {

		return DBPediaService.getResourceNames(model, Locale.GERMAN);
	}

	/**
	 * Creates a {@link Category}, sets English and German names of it and
	 * returns it. Note that this method does not assign questions of the
	 * {@link Category}!
	 * 
	 * @param nameEN
	 *            English name
	 * @param nameDE
	 *            German name
	 * @return created {@link Category} itself
	 */
	private static Category createCategory(String nameEN, String nameDE) {

		Category category = new Category();
		category.setNameEN(nameEN);
		category.setNameDE(nameDE);
		return category;
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
	private static Question createQuestion(BigDecimal maxtime, String textEN,
			String textDE) {

		Question question = new Question();
		question.setMaxTime(maxtime);
		question.setTextEN(textEN);
		question.setTextDE(textDE);
		return question;
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
	private static Choice createChoice(Question question, String textEN,
			String textDE, boolean correctAnswer) {

		Choice choice = new Choice();
		choice.setQuestion(question);
		choice.setTextEN(textEN);
		choice.setTextDE(textDE);
		choice.setCorrectAnswer(correctAnswer);
		return choice;
	}

	/**
	 * Creates an {@link ArrayList} of {@link Question}s from given parameters
	 * and returns it.
	 * 
	 * @param questions
	 *            from which the {@link List} created
	 * @return created {@link ArrayList}
	 */
	private static List<Question> createQuestionList(Question... questions) {

		List<Question> questionList = new ArrayList<Question>();

		for (Question q : questions)
			questionList.add(q);

		return questionList;
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
	private static List<Choice> createChoiceList(Question question,
			List<String> trueENTextList, List<String> trueDETextList,
			List<String> falseENTextList, List<String> falseDETextList) {

		List<Choice> choiceList = new ArrayList<Choice>();
		int sizeTrue = trueENTextList.size();

		for (int i = 0; i < sizeTrue; i++) {

			Choice choice = createChoice(question, trueENTextList.get(i),
					trueDETextList.get(i), true);
			choiceList.add(choice);
		}

		int sizeFalse = falseENTextList.size();

		for (int i = 0; i < sizeFalse; i++) {

			Choice choice = createChoice(question, falseENTextList.get(i),
					falseDETextList.get(i), false);
			choiceList.add(choice);
		}

		Collections.shuffle(choiceList);
		return choiceList;
	}
}
