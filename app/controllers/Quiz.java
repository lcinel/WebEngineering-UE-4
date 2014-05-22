package controllers;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import models.Category;
import models.Choice;
import models.Question;
import models.QuizDAO;
import models.QuizGame;
import models.QuizUser;
import play.Logger;
import play.Play;
import play.api.Application;
import play.api.cache.Cache;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import scala.Option;
import views.html.quiz.index;
import views.html.quiz.quiz;
import views.html.quiz.quizover;
import views.html.quiz.roundover;

@Security.Authenticated(Secured.class)
public class Quiz extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result newGame() {
		createNewGame();
		return redirect(routes.Quiz.question());
	}

	@play.db.jpa.Transactional(readOnly = true)
	private static QuizGame createNewGame() {
		List<Category> allCategories = QuizDAO.INSTANCE
				.findEntities(Category.class);
		Logger.info("Start game with " + allCategories.size() + " categories.");
		QuizGame game = new QuizGame(allCategories, user());
		game.startNewRound();
		cacheGame(game);
		return game;
	}

	@SuppressWarnings("unused")
	private static String dataFilePath() {
		return Play.application().configuration()
				.getString("questions.filePath");
	}

	private static QuizUser user() {
		String userId = Secured.getAuthentication(session());
		return QuizDAO.INSTANCE.findById(Long.valueOf(userId));
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result question() {
		QuizGame game = cachedGame();
		if (currentQuestion(game) != null) {
			return ok(quiz.render(game));
		} else {
			return badRequest(Messages.get("quiz.no-current-question"));
		}
	}

	@Transactional(readOnly = true)
	private static Question currentQuestion(QuizGame game) {
		if (game != null && game.getCurrentRound() != null) {
			QuizUser user = game.getPlayers().get(0);
			return game.getCurrentRound().getCurrentQuestion(user);
		} else {
			return null;
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result addAnswer() {
		QuizGame game = cachedGame();
		Question question = currentQuestion(game);
		if (question != null) {
			processAnswerIfSent(game);
			return redirectAccordingToGameState(game);
		} else {
			return badRequest(Messages.get("quiz.no-current-question"));
		}
	}

	@Transactional
	private static void processAnswerIfSent(QuizGame game) {
		DynamicForm form = Form.form().bindFromRequest();
		QuizUser user = game.getPlayers().get(0);
		Question question = game.getCurrentRound().getCurrentQuestion(user);
		int sentQuestionId = Integer.valueOf(form.data().get("questionid"));
		if (question.getId() == sentQuestionId) {
			List<Choice> choices = obtainSelectedChoices(form, question);
			long time = Long.valueOf(form.get("timeleft"));
			game.answerCurrentQuestion(user, choices, time);
		}
	}

	@Transactional
	private static List<Choice> obtainSelectedChoices(DynamicForm form,
			Question question) {
		Map<String, String> formData = form.data();
		List<Choice> choices = new ArrayList<Choice>();
		int i = 0;
		String chosenId = null;
		while ((chosenId = formData.get("choices[" + i + "]")) != null) {
			Choice choice = getChoiceById(Integer.valueOf(chosenId), question);
			if (choice != null) {
				choices.add(choice);
			}
			i++;
		}
		return choices;
	}

	private static Choice getChoiceById(int id, Question question) {
		for (Choice choice : question.getChoices())
			if (id == choice.getId())
				return choice;
		return null;
	}

	private static Result redirectAccordingToGameState(QuizGame game) {
		if (isRoundOver(game)) {
			return redirect(routes.Quiz.roundResult());
		} else if (isGameOver(game)) {
			return redirect(routes.Quiz.endResult());
		} else {
			return redirect(routes.Quiz.question());
		}
	}

	private static boolean isGameOver(QuizGame game) {
		return game.isRoundOver() && game.isGameOver();
	}

	private static boolean isRoundOver(QuizGame game) {
		return game.isRoundOver() && !game.isGameOver();
	}

	private static void cacheGame(QuizGame game) {
		Cache.set(gameId(), game, 3600, application());
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result roundResult() {
		QuizGame game = cachedGame();
		if (game != null && isRoundOver(game)) {
			return ok(roundover.render(game));
		} else {
			return badRequest(Messages.get("quiz.no-round-result"));
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result endResult() {
		QuizGame game = cachedGame();
		if (game != null && isGameOver(game)) {
			createSoapRequest(game);
			return ok(quizover.render(game));
		} else {
			return badRequest(Messages.get("quiz.no-end-result"));
		}
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result newRound() {
		QuizGame game = cachedGame();
		if (game != null && isRoundOver(game)) {
			game.startNewRound();
			return redirect(routes.Quiz.question());
		} else {
			return badRequest(Messages.get("quiz.no-round-ended"));
		}
	}

	private static QuizGame cachedGame() {
		Option<Object> option = Cache.get(gameId(), application());
		if (option.isDefined() && option.get() instanceof QuizGame) {
			return (QuizGame) option.get();
		} else {
			return createNewGame();
		}
	}

	private static String gameId() {
		return "game." + uuid();
	}

	private static String uuid() {
		String uuid = session("uuid");
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
			session("uuid", uuid);
		}
		return uuid;
	}

	private static Application application() {
		return Play.application().getWrappedApplication();
	}

	private static void createSoapRequest(QuizGame game) {

		try {

			String userKey = "rkf4394dwqp49x";
			String data = "http://big.tuwien.ac.at/we/highscore/data";
			URL endpoint = new URL(
					"http://playground.big.tuwien.ac.at:8080/highscore/PublishHighScoreService");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			QuizUser playerOne = game.getPlayers().get(0);
			QuizUser playerTwo = game.getPlayers().get(1);

			SOAPFactory soapFactory = SOAPFactory.newInstance();

			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage message = messageFactory.createMessage();
			SOAPPart soapPart = message.getSOAPPart();

			// Names
			Name bodyName = soapFactory.createName("HighScoreRequest");
			Name quizName = soapFactory.createName("quiz");
			Name usersName = soapFactory.createName("users");
			Name userName = soapFactory.createName("user");
			Name passwordName = soapFactory.createName("password");
			Name firstNameName = soapFactory.createName("firstname");
			Name lastNameName = soapFactory.createName("lastname");
			Name birthDateName = soapFactory.createName("birthdate");

			// Create envelope
			SOAPEnvelope envelope = soapPart.getEnvelope();
			envelope.addNamespaceDeclaration("data", data);

			// Create bodyElement
			SOAPBody body = envelope.getBody();
			SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
			bodyElement.setPrefix("data");

			// Set node "UserKey" as child element of bodyElement
			SOAPElement userKeyElement = bodyElement.addChildElement("UserKey",
					"data");
			userKeyElement.addTextNode(userKey);

			// Set node "quiz" as child element of bodyElement
			SOAPElement quizElement = bodyElement.addChildElement(quizName);

			// Set node "users" as child element of quizElement
			SOAPElement usersElement = quizElement.addChildElement(usersName);

			// Set node "user" (player1) as child element of usersElement
			SOAPElement userOneElement = usersElement.addChildElement(userName);
			userOneElement.setAttribute("gender", playerOne.getGender()
					.toString());
			if (game.getWinner().equals(playerOne))
				userOneElement.setAttribute("name", "winner");
			else
				userOneElement.setAttribute("name", "loser");

			SOAPElement passwordElementOne = userOneElement
					.addChildElement(passwordName);
			SOAPElement firstNameElementOne = userOneElement
					.addChildElement(firstNameName);
			SOAPElement lastNameElementOne = userOneElement
					.addChildElement(lastNameName);
			SOAPElement birthDateElementOne = userOneElement
					.addChildElement(birthDateName);
			passwordElementOne.addTextNode("");
			firstNameElementOne.addTextNode(playerOne.getFirstName());
			lastNameElementOne.addTextNode(playerOne.getLastName());
			if (playerOne.getBirthDate() == null)
				birthDateElementOne.addTextNode("");
			else
				birthDateElementOne.addTextNode(sdf.format(playerOne
						.getBirthDate()));

			// Set node "user" (player2) as child element of usersElement
			SOAPElement userTwoElement = usersElement.addChildElement(userName);
			userTwoElement.setAttribute("gender", playerTwo.getGender()
					.toString());
			if (game.getWinner().equals(playerTwo))
				userTwoElement.setAttribute("name", "winner");
			else
				userTwoElement.setAttribute("name", "loser");

			SOAPElement passwordElementTwo = userTwoElement
					.addChildElement(passwordName);
			SOAPElement firstNameElementTwo = userTwoElement
					.addChildElement(firstNameName);
			SOAPElement lastNameElementTwo = userTwoElement
					.addChildElement(lastNameName);
			SOAPElement birthDateElementTwo = userTwoElement
					.addChildElement(birthDateName);
			passwordElementTwo.addTextNode("");
			firstNameElementTwo.addTextNode(playerTwo.getFirstName());
			lastNameElementTwo.addTextNode(playerTwo.getLastName());
			birthDateElementTwo
					.addTextNode(sdf.format(playerTwo.getBirthDate()));

			Logger.info("SOAP-Request created!");

			getSoapResponse(message, endpoint);

		} catch (Exception e) {

			Logger.error(e.getMessage(), e);
		}
	}

	private static void getSoapResponse(SOAPMessage message, URL endpoint) {

		try {

			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory
					.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory
					.createConnection();

			Logger.info("SOAP-Request sending!");
			SOAPMessage response = soapConnection.call(message, endpoint);

			Logger.info("SOAP-Response received!");

			SOAPBody body = response.getSOAPBody();

			if (body.hasFault()) {

				Logger.info("SOAP-Response contains failure!");

				SOAPFault fault = body.getFault();
				Name code = fault.getFaultCodeAsName();
				String string = fault.getFaultString();
				String actor = fault.getFaultActor();

				Logger.error("Fault at SOAP-Response:" + "\n\tFault code: "
						+ code.getQualifiedName() + "\n\tFault string: "
						+ string + "\n\tFault actor: " + actor);

				return;
			}

			@SuppressWarnings("rawtypes")
			Iterator iterator = body.getChildElements();

			if (iterator.hasNext()) {

				SOAPBodyElement uuidElement = (SOAPBodyElement) iterator.next();
				String uuid = uuidElement.getValue();

				Logger.info("UUID caught: " + uuid);
			} else {

				Logger.error("SOAP-Response has no child elements!");
			}

		} catch (Exception e) {

			Logger.error(e.getMessage(), e);
		}
	}
}
