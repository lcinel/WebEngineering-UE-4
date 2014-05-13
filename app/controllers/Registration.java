package controllers;

import models.QuizDAO;
import models.QuizUser;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.registration;

public class Registration extends Controller {

	public static Result index() {
		return ok(registration.render(Form.form(QuizUser.class)));
	}

	@play.db.jpa.Transactional
	public static Result create() {
		Form<QuizUser> form = Form.form(QuizUser.class).bindFromRequest();
		if (form.hasErrors()) {
			return badRequest(registration.render(form));
		} else {
			QuizUser user = form.get();
            QuizDAO.INSTANCE.persist(user);
			flash("registration.successful", "user.created-successfully");
			return redirect(routes.Authentication.login());
		}
	}

}
