package controllers;

import models.QuizDAO;
import models.QuizUser;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.authentication;

public class Authentication extends Controller {

	public static Result login() {
		return ok(authentication.render(Form.form()));
	}

	public static Result logout() {
		session().clear();
		return redirect(routes.Authentication.login());
	}

	@play.db.jpa.Transactional(readOnly = true)
	public static Result authenticate() {
		DynamicForm loginForm = Form.form().bindFromRequest();
		QuizUser user = obtainAuthenticatedQuizUser(loginForm);
		if (user != null) {
			Secured.addAuthentication(session(), user);
			return redirect(routes.Application.index());
		} else {
			loginForm.reject("authentication.unsuccessful");
			return badRequest(authentication.render(loginForm));
		}
	}

	private static QuizUser obtainAuthenticatedQuizUser(DynamicForm loginForm) {
		if (!loginForm.hasErrors()) {
			String userName = getUserName(loginForm);
			String password = getPassword(loginForm);
			QuizUser user = QuizDAO.INSTANCE.findByUserName(userName);
			if (user != null && user.authenticate(password)) {
				return user;
			}
		}
		return null;
	}

	private static String getPassword(DynamicForm loginForm) {
		return loginForm.data().get("password");
	}

	private static String getUserName(DynamicForm loginForm) {
		return loginForm.data().get("userName");
	}

}
