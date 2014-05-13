package models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a question, which is stored in the DB
 */
public class Question extends BaseEntity {


    private String textDE;
    private String textEN;
    private BigDecimal maxtime;

    //The category to which this question belongs to
    private Category category;


    //A list of choices belonging to this question
    private List<Choice> choices = new ArrayList<Choice>();


    /**
     * Add a wrong choice
     * @param choice
     */
    public void addWrongChoice(Choice choice) {
        choice.setQuestion(this);
        choice.setCorrectAnswer(Boolean.FALSE);
        choices.add(choice);
    }


    /**
     * Add a right choice
     * @param choice
     */
    public void addRightChoice(Choice choice) {
        choice.setQuestion(this);
        choice.setCorrectAnswer(Boolean.TRUE);
        choices.add(choice);
    }


    /**
     * Set the text attribute based on the given language. Default to English if no or an invalid language is passed
     * @param name
     * @param lang
     */
    public void setText(String name, String lang) {
        if ("de".equalsIgnoreCase(lang)) {
            this.textDE = name;
        }
        else {
            this.textEN = name;
        }
    }

    /**
     * Get the text attribute based on the given language. Default to English if no or an invalid language is passed
     * @param lang
     * @return
     */
    public String getText(String lang) {
        if ("de".equalsIgnoreCase(lang)) {
            return this.textDE;
        }
        else {
            return this.textEN;
        }
    }



    public BigDecimal getMaxTime() {
        return maxtime;
    }

    public void setMaxTime(BigDecimal maxtime) {
        this.maxtime = maxtime;
    }

    public String getTextDE() {
        return textDE;
    }

    public void setTextDE(String textDE) {
        this.textDE = textDE;
    }

    public String getTextEN() {
        return textEN;
    }

    public void setTextEN(String textEN) {
        this.textEN = textEN;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
    
    public List<Choice> getCorrectChoices() {
    	List<Choice> correct = new ArrayList<Choice>();
    	for(Choice c : choices)
    		if(c.isRight())
    			correct.add(c);
    	return correct;
    }
    
    public List<Choice> getWrongChoices() {
    	List<Choice> wrong = new ArrayList<Choice>();
    	for(Choice c : choices)
    		if(c.isWrong())
    			wrong.add(c);
    	return wrong;
    }
}
