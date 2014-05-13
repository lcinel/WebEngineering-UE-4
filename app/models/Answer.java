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
import java.util.List;

public class Answer {
	
	private long time;
	
	private QuizUser user;
	
	private List<Choice> choices;
	
	private Round round;
	
	private Question question;
	
	public Answer() {
		choices = new ArrayList<>();
	}
	
	public boolean isCorrect() {
		if(!choices.isEmpty()){
			List<Choice> correctChoices = question.getCorrectChoices();
			if(choices.size() == correctChoices.size()){
				return correctChoices.containsAll(choices);
			}
		}
		return false;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return time;
	}

	public void setPlayer(QuizUser user) {
		this.user = user;
	}

	public QuizUser getPlayer() {
		return user;
	}

	public void setChoices(List<Choice> choices) {
		this.choices = choices;
	}

	public List<Choice> getChoices() {
		return choices;
	}

	public void setRound(Round round) {
		this.round = round;
	}

	public Round getRound() {
		return round;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public Question getQuestion() {
		return question;
	}

}
