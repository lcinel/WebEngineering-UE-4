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
package data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import play.db.jpa.Transactional;
import models.Category;
import models.Choice;
import models.Question;
import models.QuizDAO;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class JSONDataInserter {

	private static Gson createGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Category.class, new CategoryDeserializer());
		gsonBuilder.registerTypeAdapter(Question.class,	new QuestionDeserializer());
		gsonBuilder.registerTypeAdapter(Choice.class,	new ChoiceDeserializer());
		return gsonBuilder.create();
	}

	public static List<Category> loadCategoryData(InputStream inputStream) {
		Gson gson = createGson();
		Type collectionType = new TypeToken<List<Category>>() {}.getType();
		
		List<Category> categories = gson.fromJson(
				new InputStreamReader(inputStream, Charsets.UTF_8), collectionType);

		return categories;
	}
	
	@Transactional
	public static void insertData(InputStream inputStream) {
		List<Category> jsonCategories = loadCategoryData(inputStream);
		for(Category category : jsonCategories)
			QuizDAO.INSTANCE.persist(category);
	}
}

class CategoryDeserializer implements JsonDeserializer<Category> {

	@Override
	public Category deserialize(JsonElement json, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		
		Category category = new Category();
		JsonObject object = json.getAsJsonObject();
		
		category.setNameDE(object.get("nameDE").getAsString());
		category.setNameEN(object.get("nameEN").getAsString());
		
		for (JsonElement jsonQuestion : object.get("questions").getAsJsonArray()) {
			Question question = context.deserialize(jsonQuestion,
					new TypeToken<Question>() {}.getType());
			category.addQuestion(question);
		}

		return category;
	}

}

class QuestionDeserializer implements JsonDeserializer<Question> {

	@Override
	public Question deserialize(JsonElement json, Type type,
			JsonDeserializationContext context) throws JsonParseException {

		Question question = new Question();

		JsonObject object = json.getAsJsonObject();
		question.setTextDE(object.get("textDE").getAsString());
		question.setTextEN(object.get("textEN").getAsString());
		question.setMaxTime(object.get("maxTime").getAsBigDecimal());

		for (JsonElement wrongChoice : object.get("wrongChoices").getAsJsonArray()) {
			Choice choice = context.deserialize(wrongChoice,
					new TypeToken<Choice>() {}.getType());
			question.addWrongChoice(choice);
		}
		
		for (JsonElement correctChoice : object.get("correctChoices")
				.getAsJsonArray()) {
			Choice choice = context.deserialize(correctChoice,
					new TypeToken<Choice>() {}.getType());
			question.addRightChoice(choice);
		}

		return question;
	}
}

class ChoiceDeserializer implements JsonDeserializer<Choice> {

	@Override
	public Choice deserialize(JsonElement json, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		
		Choice choice = new Choice();
		JsonObject object = json.getAsJsonObject();
		choice.setTextDE(object.get("textDE").getAsString());
		choice.setTextEN(object.get("textEN").getAsString());
		
		return choice;
	}

}
