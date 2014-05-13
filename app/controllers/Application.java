package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

public class Application extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result index() {
		return redirect(routes.Quiz.index());
	}
}
