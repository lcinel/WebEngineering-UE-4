package models;

/**
 * Represents a choice which is stored in the DB
 */
public class Choice extends BaseEntity {


    private String textDE;
    private String textEN;

    //Indicates whether this is a correct choice or not
    private Boolean correctAnswer;

    //The question this choice belongs to
    private Question question;

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

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
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

    public Boolean getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Boolean correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public Boolean isWrong() {
    	return !correctAnswer;
    }
    
    public Boolean isRight() {
    	return correctAnswer;
    }
}
