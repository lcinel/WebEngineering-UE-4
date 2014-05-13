package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

@Entity
public class QuizUser extends BaseEntity {

	public enum Gender {
		male, female
	}

    //@TODO - as soon as the id attribute has been introduced to the base entity - remove the ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


	@Constraints.Required
	@Constraints.MinLength(4)
	@Constraints.MaxLength(8)
	private String userName;

	@Constraints.Required
	@Constraints.MinLength(4)
	@Constraints.MaxLength(8)
	private String password;

	private String firstName;
	private String lastName;
	private Date birthDate;
	private Gender gender;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getName() {
		return userName;
	}

	public void setName(String name) {
		this.userName = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		if (userNameIsTaken())
			errors.add(new ValidationError("userName", "user.user-name-not-unique"));
		return errors.isEmpty() ? null : errors;
	}

	private boolean userNameIsTaken() {
		return QuizDAO.INSTANCE.findByUserName(userName) != null;
	}

	public boolean authenticate() {
		QuizUser user = QuizDAO.INSTANCE.findByUserName(userName);
		return user != null && user.authenticate(password);
	}

	public boolean authenticate(String password) {
		return this.getPassword().equals(password);
	}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}