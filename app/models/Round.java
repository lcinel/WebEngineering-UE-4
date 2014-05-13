/**
 * <copyright>
 *
 * Copyright (c) 2014 http://www.big.tuwien.ac.at All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * </copyright>
 */
package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Round {
	
	private Map<QuizUser, List<Answer>> answers;
	
	private List<Question> questions;
	
	private QuizUser winner =  null;
	
	private List<QuizUser> players;
	
	public void initialize(List<QuizUser> players, List<Question> questions) {
		answers = new HashMap<>();
		for(QuizUser player : players){
			answers.put(player, new ArrayList<Answer>());
		}
		
		if(questions != null){
			this.questions = questions;
		}else{
			this.questions = new ArrayList<Question>();
		}
		
		this.players = players;
	}

	public Answer getAnswer(int questionnumber, QuizUser player) {
		
		if(answers.containsKey(player)){
			List<Answer> playerAnswers = answers.get(player);
			if(questionnumber < playerAnswers.size()){
				return playerAnswers.get(questionnumber);
			}
		}
		return null;
	}

	public void answerCurrentQuestion(List<Choice> choices, long time, QuizUser player) {
		if(answers.containsKey(player)){
			//int questionnumber = answers.get(player).size();
			Answer answer = new Answer();
			answer.setPlayer(player);
			answer.setTime(time);
			answer.setChoices(choices);
			answer.setQuestion(getCurrentQuestion(player));
			answers.get(player).add(answer);
		}
		if(areAllQuestionsAnswered()){
			determineWinner();
		}
	}
	
	private void determineWinner() {

		ArrayList<RoundStatistics> playerStats = new ArrayList<>(players.size());
		for(QuizUser player : players){
			List<Answer> playerAnswers = answers.get(player);
			RoundStatistics statistics = new RoundStatistics();
			statistics.player = player;
			for(Answer answer : playerAnswers){
				if(answer.isCorrect()){
					statistics.correctQuestions++;
				}
				statistics.totalTime += answer.getTime();
			}
			playerStats.add(statistics);
		}
		
		ArrayList<RoundStatistics> bestStatistics = new ArrayList<>();
		bestStatistics.add(playerStats.get(0));
		for(RoundStatistics statistics : playerStats){
			if(statistics != bestStatistics.get(0)){
				RoundStatistics other = bestStatistics.get(0);
				if(statistics.correctQuestions > other.correctQuestions){
					bestStatistics.clear();
					bestStatistics.add(statistics);
				}else if(statistics.correctQuestions == other.correctQuestions){
					if(statistics.totalTime < other.totalTime){
						bestStatistics.clear();
						bestStatistics.add(statistics);
					}else if(statistics.totalTime == other.totalTime){
						bestStatistics.add(statistics);
					}
				}
			}
		}
		
		if(bestStatistics.size() == 1){
			winner = bestStatistics.get(0).player;
		}
		
	}

	public QuizUser getRoundWinner() {
		return winner;
	}

	public Question getQuestion(int questionNumber) {
		if(questionNumber < questions.size()){
			return questions.get(questionNumber);
		}
		return null;
	}

	public boolean areAllQuestionsAnswered() {
		boolean allQuestionsAnswered = true; 
		for(QuizUser user : answers.keySet()){
			allQuestionsAnswered = allQuestionsAnswered && answers.get(user).size() == questions.size();
		}
		return allQuestionsAnswered;
	}

	public Question getCurrentQuestion(QuizUser player) {
		if(answers.containsKey(player)){
			return getQuestion(answers.get(player).size());
		}
		return null;
	}

	public List<Question> getQuestions() {
		return questions;
	}

}

class RoundStatistics{
	QuizUser player;
	long totalTime = 0;
	int correctQuestions = 0;
}
