package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * Represents a category, which is stored in the DB
 */
@Entity
public class Category extends BaseEntity {


    private String nameDE;
    private String nameEN;


    //A list of questions in this category
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "category")
    private List<Question> questions = new ArrayList<Question>();

    /**
     * Add a new question to the category
     * @param question
     */
    public void addQuestion(Question question) {
        question.setCategory(this);
        questions.add(question);
    }

    /**
     * Set the name attribute based on the given language. Default to English if no or an invalid language is passed
     * @param name
     * @param lang
     */
    public void setName(String name, String lang) {
        if ("de".equalsIgnoreCase(lang)) {
            this.nameDE = name;
        }
        else {
            this.nameEN = name;
        }
    }

    /**
     * Get the name attribute based on the given language. Default to English if no or an invalid language is passed
     * @param lang
     * @return
     */
    public String getName(String lang) {
        if ("de".equalsIgnoreCase(lang)) {
            return this.nameDE;
        }
        else {
            return this.nameEN;
        }
    }


    public String getNameDE() {
        return nameDE;
    }

    public void setNameDE(String nameDE) {
        this.nameDE = nameDE;
    }

    public String getNameEN() {
        return nameEN;
    }

    public void setNameEN(String nameEN) {
        this.nameEN = nameEN;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
