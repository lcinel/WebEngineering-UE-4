package models;

import static java.lang.Math.random;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuizGame {
	private static int NUM_ROUNDS = 5;
	private static int NUM_QUESTIONS = 3;

	private List<Round> rounds;

	private QuizUser human;
	private QuizUser computer;

	private List<Category> categories;

	/**
	 * list of already chosen categories during the game
	 */
	private List<Category> chosenCategories;

	public QuizGame(List<Category> categories) {
		this.categories = categories;
		initializeQuizGame();
		initializePlayers();
	}

	/**
	 * Create a game with one {@code human} {@link User} against a computer
	 * user.
	 * 
	 * @param factory
	 *            factory to use.
	 * @param human
	 *            the human user.
	 */
	public QuizGame(List<Category> categories, QuizUser human) {
		this.human = human;
		this.categories = categories;
		this.computer = createComputerPlayer();
		initializeQuizGame();
	}

	private void initializeQuizGame() {
		rounds = new ArrayList<Round>();
		chosenCategories = new ArrayList<>();
	}

	private void initializePlayers() {
		human = createHumanPlayer();
		computer = createComputerPlayer();
	}

	private QuizUser createHumanPlayer() {
		QuizUser user = new QuizUser();
		user.setName("Spieler 1");
		return user;
	}

	private QuizUser createComputerPlayer() {
		QuizUser user = new QuizUser();
		user.setName("Spieler 2");
		return user;
	}

	public Round getCurrentRound() {
		return rounds.get(rounds.size() - 1);
	}

	public int getCurrentRoundCount() {
		return rounds.size();
	}

	public void startNewRound() {
		Category category = chooseCategory();

        //Category might not be attached to Peristence Context any more - reattach if necessary
        category = QuizDAO.INSTANCE.merge(category);

		List<Question> questions = chooseQuestions(category);
		Round round = new Round();
		round.initialize(getPlayers(), questions);
		rounds.add(round);
	}

	private Category chooseCategory() {
		List<Category> availableCategories = getAvailableCategories();
		int randomCategoryIndex = (int) round(random()
				* maxIndex(availableCategories));
		Category category = availableCategories.get(randomCategoryIndex);
		chosenCategories.add(category);
		return category;
	}

	private List<Category> getAvailableCategories() {
		if (chosenCategories.size() == categories.size())
			chosenCategories.clear();
		List<Category> availableCategories = new ArrayList<Category>();
		for (Category category : categories) {
			if (!chosenCategories.contains(category)) {
				availableCategories.add(category);
			}
		}
		return availableCategories;
	}

	private int maxIndex(List<?> list) {
		return list.size() - 1;
	}

	private List<Question> chooseQuestions(Category category) {

		List<Question> questions = new ArrayList<>();
		List<Question> availableQuestions = new ArrayList<>(
				category.getQuestions());
		for (int i = 0; i < Math.min(NUM_QUESTIONS, availableQuestions.size()); i++) {
			int randomQuestionIndex = (int) round(random()
					* maxIndex(availableQuestions));
			Question question = availableQuestions.get(randomQuestionIndex);
			questions.add(question);
			availableQuestions.remove(question);
		}
		return questions;
	}

	public void answerCurrentQuestion(QuizUser player, List<Choice> answers,
			long time) {
		getCurrentRound().answerCurrentQuestion(answers, time, player);
		if (player == human)
			doAutomaticAnswerOfComputer();
	}

	private void doAutomaticAnswerOfComputer() {
		Round round = getCurrentRound();
		Question question = round.getCurrentQuestion(computer);
		// correct cast of complete result to long
		long time = (long) (random() * question.getMaxTime().doubleValue());
		List<Choice> answers = chooseComputerAnswers(question);
		round.answerCurrentQuestion(answers, time, computer);
	}

	private List<Choice> chooseComputerAnswers(Question question) {
		if (random() < 0.5) {
			return Collections.unmodifiableList(question.getCorrectChoices());
		} else {
			return Collections.emptyList();
		}
	}

	public boolean isGameOver() {
		Round currentRound = getCurrentRound();
		return rounds.size() >= NUM_ROUNDS
				&& currentRound.areAllQuestionsAnswered();
	}

	public boolean isRoundOver() {
		return getCurrentRound().areAllQuestionsAnswered();
	}

	public List<QuizUser> getPlayers() {
		return Arrays.asList(new QuizUser[] { human, computer });
	}

	public int getWonRounds(QuizUser player) {
		int counter = 0;
		for (Round round : rounds) {
			if (round.getRoundWinner() != null && round.getRoundWinner().equals(player)) {
				counter++;
			}
		}
		return counter;
	}

	public QuizUser getWinner() {
		if (isGameOver()) {
			ArrayList<QuizUser> bestUsers = new ArrayList<>();
			int bestCount = 0;

			for (QuizUser player : getPlayers()) {
				int count = 0;
				for (Round round : rounds) {
					// added missing null check
					if (round.getRoundWinner() != null && round.getRoundWinner().equals(player))
						count++;
				}
				
				if (count > bestCount) {
					bestUsers.clear();
					bestUsers.add(player);
					bestCount = count;
				} else if (count == bestCount) {
					bestUsers.add(player);
				}
			}

			if (bestUsers.size() == 1)
				return bestUsers.get(0);
		}
		return null;
	}
}
